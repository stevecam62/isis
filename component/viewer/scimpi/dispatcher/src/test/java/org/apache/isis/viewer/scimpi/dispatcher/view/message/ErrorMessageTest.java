package org.apache.isis.viewer.scimpi.dispatcher.view.message;

import java.util.Stack;

import org.apache.isis.viewer.scimpi.dispatcher.processor.Snippet;
import org.apache.isis.viewer.scimpi.dispatcher.processor.SwfTag;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TagAttributes;
import org.apache.isis.viewer.scimpi.dispatcher.processor.TemplateProcessor;
import org.apache.isis.viewer.scimpi.dispatcher.view.TestRequestState;
import org.htmlparser.nodes.TagNode;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

public class ErrorMessageTest {

    @Test
    public void test() {
        TagNode tagNode = new TagNode();

        TagAttributes tagAttributes = new TagAttributes(tagNode, null);
        Stack<Snippet> snippets = new Stack<Snippet>();
        Snippet snippet = new SwfTag("", tagAttributes , SwfTag.EMPTY, "", "") ;
        snippets.push(snippet);
        
        TestRequestState state = new TestRequestState();
        TemplateProcessor templateProcessor = new TemplateProcessor(null, state, null, snippets, null);
        
        //       tagNode.setTagName("swf:error-message");
        
        
        
        ErrorMessage processor = new ErrorMessage();        
        processor.process(templateProcessor, state);
        String content = templateProcessor.popBuffer().toString();
        
        assertThat(content, equalTo("an error"));
    }

}

