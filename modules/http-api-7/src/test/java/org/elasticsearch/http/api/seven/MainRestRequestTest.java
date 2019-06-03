package org.elasticsearch.http.api.seven;


import org.elasticsearch.http.ModeledHttpResponse;
import org.junit.Assert;
import org.junit.Test;

@ModeledHttpResponse("test")
public class MainRestRequestTest {


    @Test
    public void test() {
        Assert.assertEquals(1,1);
    }
}
