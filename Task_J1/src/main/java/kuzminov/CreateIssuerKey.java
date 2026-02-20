package kuzminov;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

public class CreateIssuerKey {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(8192);
        KeyPair keyPair = kpg.generateKeyPair();

        PrivateKey privateKey = keyPair.getPrivate();
        Files.write(Paths.get("issuerKey.key"), privateKey.getEncoded());
    }
}
