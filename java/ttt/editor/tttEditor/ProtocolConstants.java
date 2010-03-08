package ttt.editor.tttEditor;

/**
 * Constants defined in the TTT Protocol.
 */
public final class ProtocolConstants {
    
    public final static int FramebufferUpdate = 0, SetColourMapEntries = 1, Bell = 2, ServerCutText = 3;

    public final static int SetPixelFormat = 0, FixColourMapEntries = 1, SetEncodings = 2,
            FramebufferUpdateRequest = 3, KeyboardEvent = 4, PointerEvent = 5, ClientCutText = 6;

    // only used by TTT protocol
    public static final int EncodingTTTCursorPosition = 17, EncodingTTTXCursor = 18, EncodingTTTRichCursor = 19,
            EncodingUpdate = 64, EncodingTimestamp = 128, RECT = 20, LINE = 21, FREE = 22, HIGHLIGHT = 23, REMOVE = 24,
            REMOVE_ALL = 25, TEXT = 27, EncodingRecording = 30, EncodingBlankPageOn = 31, EncodingBlankPageOff = 32;

    public static final int EncodingBlankPage = 33;
    
    // used by both, TTT and RFB protocol
    public final static int EncodingRaw = 0, EncodingCopyRect = 1, EncodingRRE = 2, EncodingCoRRE = 4,
            EncodingHextile = 5, EncodingZlib = 6, EncodingTight = 7;

    // only used by RFB protocol
    public final static int EncodingCompressLevel0 = 0xFFFFFF00, EncodingQualityLevel0 = 0xFFFFFFE0,
            EncodingXCursor = 0xFFFFFF10, EncodingRichCursor = 0xFFFFFF11, EncodingLastRect = 0xFFFFFF20,
            EncodingNewFBSize = 0xFFFFFF21;

    
    public static final int EXTENSION_INDEX_TABLE = 1;
    

}