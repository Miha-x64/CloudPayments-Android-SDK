package ru.cloudpayments.sdk.cp_card;

import android.text.TextUtils;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public final class CPCard {

    private final String number;
    private final String expDate;
    private final String cvv;

    private static final String KEY_VERSION = "04";
    private static final X509EncodedKeySpec PUBLIC_KEY = new X509EncodedKeySpec(Base64.decode(
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArBZ1NNjvszen6BNWsgyDUJvDUZDtvR4jKNQtEwW1iW7hqJr0TdD8hgTxw3DfH+Hi/7ZjSNdH5EfChvgVW9wtTxrvUXCOyJndReq7qNMo94lHpoSIVW82dp4rcDB4kU+q+ekh5rj9Oj6EReCTuXr3foLLBVpH0/z1vtgcCfQzsLlGkSTwgLqASTUsuzfI8viVUbxE1a+600hN0uBh/CYKoMnCp/EhxV8g7eUmNsWjZyiUrV8AA/5DgZUCB+jqGQT/Dhc8e21tAkQ3qan/jQ5i/QYocA/4jW3WQAldMLj0PA36kINEbuDKq8qRh25v+k4qyjb7Xp4W2DywmNtG3Q20MQIDAQAB",
        Base64.NO_WRAP
    ));

    public CPCard(String number) throws IllegalArgumentException {
        if (!isValidNumber(number))
            throw new IllegalArgumentException("Card number is not correct.");

        this.number = number;
        this.expDate = null;
        this.cvv = null;
    }

    public CPCard(String number, String expDate, String cvv) throws IllegalArgumentException {
        if (!isValidNumber(number))
            throw new IllegalArgumentException("Card number is not correct.");

        if (!isValidExpDate(expDate))
            throw new IllegalArgumentException("Expiration date is not correct.");

        this.number = number;
        this.expDate = expDate;
        this.cvv = cvv;
    }

    /**
     * @return Тип карты
     */
    public String getType() {
        return getType(number);
    }

    /**
     * @return Тип карты
     */
    private String getType(String number) {
        return CPCardType.toString(CPCardType.getType(number));
    }

    /**
     * Валидация номера карты
     * @deprecated невалидные номера карт явно запрещены конструктором
     */
    public boolean isValidNumber() {
        return isValidNumber(number);
    }

    /**
     * Валидация номера карты
     * @return
     */
    public static boolean isValidNumber(String number) {
        boolean res = false;
        int sum = 0;
        int i;
        number = prepareCardNumber(number);
        if (TextUtils.isEmpty(number)) {
            return false;
        }
        if (number.length() % 2 == 0) {
            for (i = 0; i < number.length(); i += 2) {
                int c = Integer.parseInt(number.substring(i, i + 1));
                c *= 2;
                if (c > 9) {
                    c -= 9;
                }
                sum += c;
                sum += Integer.parseInt(number.substring(i + 1, i + 2));
            }
        } else {
            for (i = 1; i < number.length(); i += 2) {
                int c = Integer.parseInt(number.substring(i, i + 1));
                c *= 2;
                if (c > 9) {
                    c -= 9;
                }
                sum += c;
                sum += Integer.parseInt(number.substring(i - 1, i));
            }
//			adding last character
            sum += Integer.parseInt(number.substring(i - 1, i));
        }
        //final check
        if (sum % 10 == 0) {
            res = true;
        }
        return res;
    }

    /**
     * Валидация даты
     * @return
     */
    public boolean isValidExpDate() {
        return expDate != null; // constructor already checks whether it is valid, just mind it's optional
    }

    /**
     * Валидация даты
     * @return
     */
    public static boolean isValidExpDate(String expDate) {
        if (expDate.length() != 4) {
            return false;
        }

        DateFormat format = new SimpleDateFormat("MMyy", Locale.ENGLISH);
        format.setLenient(false);
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(format.parse(expDate));
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            return new Date().before(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Генерим криптограму для карты
     * @param publicId
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     */
    public String cardCryptogram(String publicId) throws UnsupportedEncodingException,
            NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException,
            IllegalBlockSizeException, InvalidKeyException {
        return cardCryptogram(number, expDate, cvv, publicId);
    }

    /**
     * Генерим криптограму для карты
     * @param cardNumber
     * @param cardExp
     * @param cardCvv
     * @param publicId
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     */
    private String cardCryptogram(String cardNumber, String cardExp, String cardCvv, String publicId) throws UnsupportedEncodingException,
            NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException,
            IllegalBlockSizeException, InvalidKeyException {

        cardNumber = prepareCardNumber(cardNumber);
        String shortNumber = cardNumber.substring(0, 6) + cardNumber.substring(cardNumber.length() - 4);
        String exp = cardExp.substring(2, 4) + cardExp.substring(0, 2);
        String s = cardNumber + "@" + exp + "@" + cardCvv + "@" + publicId;
        byte[] bytes = s.getBytes("ASCII");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        SecureRandom random = new SecureRandom();
        cipher.init(Cipher.ENCRYPT_MODE, getRSAKey(), random);
        byte[] crypto = cipher.doFinal(bytes);
        return "01" + shortNumber + exp + KEY_VERSION + Base64.encodeToString(crypto, Base64.NO_WRAP);
    }

    /**
     * Генерим криптограму для CVV
     * @param cardCvv
     * @return
     * @throws UnsupportedEncodingException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     * @throws InvalidKeyException
     */
    public static String cardCryptogramForCVV(String cardCvv) throws UnsupportedEncodingException,
            NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException,
            IllegalBlockSizeException, InvalidKeyException {

        byte[] bytes = cardCvv.getBytes("ASCII");
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        SecureRandom random = new SecureRandom();
        cipher.init(Cipher.ENCRYPT_MODE, getRSAKey(), random);
        byte[] crypto = cipher.doFinal(bytes);
        return "03" + KEY_VERSION + Base64.encodeToString(crypto, Base64.NO_WRAP);
    }

    private static String prepareCardNumber(String cardNumber) {
        return cardNumber.replaceAll("\\s", "");
    }

    private static PublicKey getRSAKey() {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(PUBLIC_KEY);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }
}
