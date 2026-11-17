package ua.in.quireg.chan.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite(AllTests.class.getName());
        //$JUnit-BEGIN$
        suite.addTestSuite(UriUtilsTest.class);
        suite.addTestSuite(AttachmentInfoTest.class);
        suite.addTestSuite(PostResponseParserTest.class);
        suite.addTestSuite(EmojiCaptchaResponseParserTest.class);
        suite.addTestSuite(MakabaSendPostMapperTest.class);
        suite.addTestSuite(HtmlUtilsTest.class);
        suite.addTestSuite(ThreadPostUtilsTest.class);
        //$JUnit-END$
        return suite;
    }

}
