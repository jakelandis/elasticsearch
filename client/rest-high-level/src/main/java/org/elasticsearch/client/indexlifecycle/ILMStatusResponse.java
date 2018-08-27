package org.elasticsearch.client.indexlifecycle;

import org.elasticsearch.action.admin.indices.shrink.ShrinkAction;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.XContentParser;

import java.util.EnumSet;

/**
 * The current status of index lifecycle management. See {@link OperationMode} for available statuses.
 */
public class ILMStatusResponse {

    private final OperationMode operationMode;
    @SuppressWarnings("unchecked")
    private static final ConstructingObjectParser<ILMStatusResponse, Void> PARSER = new ConstructingObjectParser<>(
        "operation_mode", a -> new ILMStatusResponse((String) a[0]));

    static {
        PARSER.declareString(ConstructingObjectParser.constructorArg(), new ParseField("operation_mode"));
    }

    //package private for testing
    ILMStatusResponse(String operationMode) {
        this.operationMode = OperationMode.fromString(operationMode);
    }

    public OperationMode getOperationMode(){
        return operationMode;
    }

    public static ILMStatusResponse fromXContent(XContentParser parser) {
        return PARSER.apply(parser, null);
    }

    /**
     * Enum representing the different modes that Index Lifecycle Service can operate in.
     */
    public enum OperationMode {
        /**
         * This represents a state where no policies are executed
         */
        STOPPED,

        /**
         * this represents a state where only sensitive actions (like {@link ShrinkAction}) will be executed
         * until they finish, at which point the operation mode will move to <code>STOPPED</code>.
         */
        STOPPING,

        /**
         * Normal operation where all policies are executed as normal.
         */
        RUNNING;

        static OperationMode fromString(String string){
            return EnumSet.allOf(OperationMode.class).stream()
                .filter(e -> string.equalsIgnoreCase(e.name())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("%s is not a valid operation_mode", string)));
        }

    }

}
