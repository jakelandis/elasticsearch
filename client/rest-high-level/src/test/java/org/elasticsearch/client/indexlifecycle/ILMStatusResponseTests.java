package org.elasticsearch.client.indexlifecycle;


import org.elasticsearch.protocol.xpack.indexlifecycle.OperationMode;
import org.elasticsearch.test.ESTestCase;
import org.hamcrest.CoreMatchers;

import java.util.EnumSet;
import java.util.stream.Collectors;

public class ILMStatusResponseTests extends ESTestCase {

    public void testClientServerStatuses(){
        assertEquals(
            EnumSet.allOf(ILMStatusResponse.OperationMode.class).stream().map(Enum::name).collect(Collectors.toSet()),
            EnumSet.allOf(OperationMode.class).stream().map(Enum::name).collect(Collectors.toSet()));
    }

    public void testFromName(){
        EnumSet.allOf(ILMStatusResponse.OperationMode.class)
            .forEach(e -> assertEquals(ILMStatusResponse.OperationMode.fromString(e.name()), e));
    }

    public void testInvalidStatus(){
        String invalidName = randomAlphaOfLength(10);
       Exception e = expectThrows(IllegalArgumentException.class, () -> ILMStatusResponse.OperationMode.fromString(invalidName));
       assertThat(e.getMessage(), CoreMatchers.containsString(invalidName + " is not a valid operation_mode"));
    }

    public void testValidStatuses(){
        EnumSet.allOf(ILMStatusResponse.OperationMode.class)
            .forEach(e -> assertEquals(new ILMStatusResponse(e.name()).getOperationMode(), e));
    }
}
