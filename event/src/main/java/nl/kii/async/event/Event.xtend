package nl.kii.async.event

import java.lang.annotation.Target
import nl.kii.async.annotation.Hot
import nl.kii.async.annotation.Uncontrolled
import nl.kii.async.publish.BasicPublisher
import nl.kii.async.publish.Publisher
import nl.kii.async.stream.Stream
import nl.kii.async.stream.StreamExtensions
import org.eclipse.xtend.lib.macro.AbstractFieldProcessor
import org.eclipse.xtend.lib.macro.Active
import org.eclipse.xtend.lib.macro.TransformationContext
import org.eclipse.xtend.lib.macro.declaration.MutableFieldDeclaration
import org.eclipse.xtend.lib.macro.declaration.Visibility
import org.eclipse.xtext.xbase.lib.Procedures.Procedure0
import org.eclipse.xtext.xbase.lib.Procedures.Procedure1
import nl.kii.async.SuspendableProcedures

/**
 * Add an Event listener to a class. This allows you to listen to events from this class
 * with multiple listeners, and publish messages to all listeners.
 * <p>
 * The annotation performs the following steps:
 * <ol>
 * <li>it adds a protected method with the name of the field, that lets you pass an item of the type of the field,
 * this method posts an event to all listeners
 * <li>it adds a public method on + [name of the field], that lets other classes register a listener for these events
 * <li>it adds a public method [name of the field] + stream, that lets you listen for events as a hot uncontrolled stream
 * <li>it creates a protected publisher that keeps track of all listening streams
 * <li>it removes the actual field, since it is only meant as instructive to the annotation
 * </ol>
 * <p>
 * Note: If you make the Event of type Void, that makes it an untyped event, and you can call it without parameters.
 * <p>
 * Example:
 * <p>
 * <pre>
 * class RSSFeed {
 * 
 *    \@Event Article newArticle
 * 
 *    def someMethod() {
 *       ...
 *       // we want to publish some new article to all listeners
 *       article = loadedFeed.articles.head
 *       newArticle(article)
 *       ...
 *    }
 * 
 * }
 * 
 * class RSSFeedResponder {
 * 
 *    def someOtherMethod() {
 *       ...
 *       // listen for articles coming in with a closure
 *       rssFeed.onNewArticle [
 *           
 *       ]
 * 
 * 		 // you can also get the direct stream, by not passing a closure:
 * 		rssFeed.newArticleStream
 *        .effect [ println('got article' + it) ]
 *        .start
 *       ...
 *    }
 * 
 * }
 * </pre>
 */
@Active(EventProcessor)
@Target(FIELD)
annotation Event {
}

class EventProcessor extends AbstractFieldProcessor {
	
	/** Process a field annotated with @Event */
	override doTransform(MutableFieldDeclaration field, extension TransformationContext context) {
		
		val cls = field.declaringType
		val isInterface = findInterface(cls.qualifiedName) !== null
		val hasPayload = field.type != Void.newTypeReference
		val publisherType = if(hasPayload) field.type else Boolean.newTypeReference
		
		val publisherFieldName = '__' + field.simpleName + 'EventPublisher'

		if(!isInterface) {
			
			// add a publisher of the type of the field
			cls.addField(publisherFieldName) [
				docComment = '''Internal publisher of «field.simpleName» events.'''
				primarySourceElement = field
				type = Publisher.newTypeReference(publisherType)
				visibility = Visibility.PROTECTED
				transient = true
			]


			// add a method for publishing the event, with the name of the field, and as a parameter the type of the field
			cls.addMethod(field.simpleName) [
				docComment = '''Fire the «field.simpleName» event for any listeners.'''
				primarySourceElement = field
				if(hasPayload) {
					val fieldParameterName = field.type.simpleName.toFirstLower
					addParameter(fieldParameterName, field.type)
					body = '''
						if(«publisherFieldName» == null) return;
						«publisherFieldName».publish(«fieldParameterName»);
					'''
				} else {
					body = '''
						if(«publisherFieldName» == null) return;
						«publisherFieldName».publish(true);
					'''
				}
			]

		}
		
		// add a method for listening to the method as a stream. It lazily initialises the event publisher.
		val streamMethodName = field.simpleName + 'Stream'
		
		cls.addMethod(streamMethodName) [
			//primarySourceElement = field
			docComment = '''
				Stream «field.simpleName» events as they occur. May be called by multiple listeners. Close the stream to unsubscribe. 
				Note: Failing to unsubscribe prevents the subscription to be garbage collected, which means you leak memory.
			'''
			if(!isInterface) primarySourceElement = field
			addAnnotation(Hot.newAnnotationReference)
			addAnnotation(Uncontrolled.newAnnotationReference)
			returnType = Stream.newTypeReference(publisherType, publisherType)
			if(!isInterface) {
				body = '''
					if(«publisherFieldName» == null) {
						«publisherFieldName» = new «BasicPublisher.newTypeReference(publisherType)»();
						«publisherFieldName».start();
					} 
					return «publisherFieldName».subscribe();
				'''
			}
		]

		// add a method for listening to the method with a handler. Wraps the stream method.
		cls.addMethod('on' + field.simpleName.toFirstUpper) [
			primarySourceElement = cls
			docComment = '''
				Listen for «field.simpleName» events. May be called by multiple listeners.
				Note: do not forget to stop listening, otherwise the listener is never released,
				which means you leak memory.
				@return unsubscribeFn. Call (.apply()) this Procedure to stop listening.
			'''
			if(!isInterface) primarySourceElement = field
			val handlerParameterName = field.simpleName + 'Handler'
			addParameter(handlerParameterName, SuspendableProcedures.Procedure1.newTypeReference(publisherType))
			returnType = Procedure0.newTypeReference
			if(!isInterface) {
				body = '''
					«Stream.newTypeReference(publisherType, publisherType)» stream = «streamMethodName»();
					«Stream.newTypeReference(publisherType, publisherType)» effect = «StreamExtensions».effect(stream, «handlerParameterName»);
					«StreamExtensions».start(effect);
					return new «Procedures.Procedure0»() {
						@Override public void apply() {
							effect.close();
						}
					};
				'''
			}
		]

		// remove the annotated field, since it is only instructional for creating the event publisher and methods
		field.remove
		
	}
	
}
