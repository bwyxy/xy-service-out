import com.xy.common.utils.EncryptionUtils;

import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

public class Encryption {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        SecretKey secretKey = EncryptionUtils.generateAESKey();

        System.out.println(secretKey);

        System.out.println(EncryptionUtils.secretKeyToBase64(secretKey));
    }
}
