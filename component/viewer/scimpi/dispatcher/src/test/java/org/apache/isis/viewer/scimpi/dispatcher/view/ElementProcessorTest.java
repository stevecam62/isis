package org.apache.isis.viewer.scimpi.dispatcher.view;

import java.util.Stack;

import org.apache.isis.viewer.scimpi.Names;
import org.apache.isis.viewer.scimpi.dispatcher.context.RequestState;
import org.apache.isis.viewer.scimpi.dispatcher.processor.Snippet;
import org.apache.isis.viewer.scimpi.dispatcher.processor.SwfTag;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagAttributes;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.htmlparser.nodes.TagNode;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;


public class ElementProcessorTest {

    @Test
    public void test() {
        ExampleElementProcessor processor = new ExampleElementProcessor();
        
        TagNode tagNode = new TagNode();
        tagNode.setTagName("swf:test");
        tagNode.setAttribute(Names.NAME, "attribute-name");
        tagNode.setAttribute(Names.ACTION, "attribute-name-2");

        TagAttributes tagAttributes = new TagAttributes(tagNode, null);
        Stack<Snippet> snippets = new Stack<Snippet>();
        Snippet snippet = new SwfTag("", tagAttributes , SwfTag.EMPTY, "", "") ;
        snippets.push(snippet);
        
        TestRequestState state = new TestRequestState();
        TemplateProcessor templateProcessor = new TemplateProcessor(null, state, null, snippets, null);
        
        processor.process(templateProcessor, state);
        String content = templateProcessor.popBuffer().toString();
        assertThat(content, equalTo("<p>html-code</p>a &amp; b &gt; c<div>attribute-name</div><div>attribute-name-2</div>"));
    }
}


    
class ExampleElementProcessor extends AbstractElementProcessor {

    public String getName() {
        return "test";
    }

    public void process(TemplateProcessor templateProcessor, RequestState state) {
        templateProcessor.appendHtml("<p>html-code</p>");
        templateProcessor.appendAsHtmlEncoded("a & b > c");
        
        final String name = templateProcessor.getOptionalProperty(NAME);
        templateProcessor.appendHtml("<div>" + name + "</div>");
        final String var = templateProcessor.getRequiredProperty(ACTION);
        templateProcessor.appendHtml("<div>" + var + "</div>");
    }

}
