package io.higgs.boson;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class BosonType {
    protected BosonType() {
    }

    public static final int BYTE = 1;
    public static final int SHORT = 2;
    public static final int INT = 3;
    public static final int LONG = 4;
    public static final int FLOAT = 5;
    public static final int DOUBLE = 6;
    public static final int BOOLEAN = 7;
    public static final int CHAR = 8;
    public static final int NULL = 9;
    public static final int STRING = 10;
    public static final int ARRAY = 11;
    public static final int LIST = 12;
    public static final int MAP = 13;
    public static final int POLO = 14;
    public static final int REFERENCE = 15;
    public static final int SET = 16;
    public static final int ENUM = 17;
    //request response flags
    public static final int REQUEST_METHOD_NAME = -127;
    public static final int REQUEST_PARAMETERS = -126;
    public static final int REQUEST_CALLBACK = -125;
    public static final int RESPONSE_METHOD_NAME = -124;
    public static final int RESPONSE_PARAMETERS = -123;
}
