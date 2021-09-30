package ru.cloudpayments.sdk.cp_card;

public class CPCardType {

    public final static int UNKNOWN = -1;
    public final static int VISA = 0;
    public final static int MASTER_CARD = 1;
    public final static int MAESTRO = 2;
    public final static int MIR = 3;
    public final static int JCB = 4;

    private static final String[] NAMES = { "Visa", "MasterCard", "Maestro", "MIR", "JCB" };
    public static String toString(int value) {
        try {
            return NAMES[value];
        } catch (ArrayIndexOutOfBoundsException e) {
            return "Unknown";
        }
    }

    public static int fromString(String value) {
        for (int i = 0, len = NAMES.length; i < len; i++)
            if (NAMES[i].equalsIgnoreCase(value))
                return i;
        return UNKNOWN;
    }

    public static int getType(String cardNumberStart) {
        if (cardNumberStart == null || cardNumberStart.isEmpty()) return UNKNOWN;

        int first = cardNumberStart.charAt(0) - '0';
        if (first == 4) return VISA;
        else if (first == 6) return MAESTRO;
        else if (cardNumberStart.length() < 2) return UNKNOWN;

        int firstTwo = 10 * first + cardNumberStart.charAt(1) - '0';

        if (firstTwo == 35) return JCB;
        else if (firstTwo == 50 || (firstTwo >= 56 && firstTwo <= 58)) return MAESTRO;
        else if (firstTwo >= 51 && firstTwo <= 55) return MASTER_CARD;
        else if (cardNumberStart.length() < 4) return UNKNOWN;

        int firstFour = 100 * firstTwo + 10 * (cardNumberStart.charAt(2) - '0') + cardNumberStart.charAt(3) - '0';
        if (firstFour >= 2200 && firstFour <= 2204) return MIR;
        if (firstFour >= 2221 && firstFour <= 2720) return MASTER_CARD;
        else return UNKNOWN;
    }
}
