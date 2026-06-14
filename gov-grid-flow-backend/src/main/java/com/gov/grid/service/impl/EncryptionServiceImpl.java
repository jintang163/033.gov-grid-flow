package com.gov.grid.service.impl;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.dto.DigitalEnvelopeDTO;
import com.gov.grid.entity.EncryptionKey;
import com.gov.grid.mapper.EncryptionKeyMapper;
import com.gov.grid.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptionServiceImpl implements EncryptionService {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";
    private static final int AES_KEY_SIZE = 256;
    private static final int IV_SIZE = 16;

    private final EncryptionKeyMapper encryptionKeyMapper;
    private final ObjectMapper objectMapper;

    @Override
    public byte[] generateAesKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE, new SecureRandom());
            SecretKey secretKey = keyGen.generateKey();
            return secretKey.getEncoded();
        } catch (GeneralSecurityException e) {
            log.error("生成AES密钥失败", e);
            throw new BusinessException("生成加密密钥失败：" + e.getMessage());
        }
    }

    @Override
    public byte[] encryptWithAes(byte[] data, byte[] aesKey) throws GeneralSecurityException {
        try {
            SecureRandom random = new SecureRandom();
            byte[] iv = new byte[IV_SIZE];
            random.nextBytes(iv);

            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptedData = cipher.doFinal(data);

            byte[] result = new byte[IV_SIZE + encryptedData.length];
            System.arraycopy(iv, 0, result, 0, IV_SIZE);
            System.arraycopy(encryptedData, 0, result, IV_SIZE, encryptedData.length);

            return result;
        } catch (GeneralSecurityException e) {
            log.error("AES加密失败", e);
            throw e;
        }
    }

    @Override
    public byte[] decryptWithAes(byte[] encryptedData, byte[] aesKey) throws GeneralSecurityException {
        try {
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE);

            byte[] data = new byte[encryptedData.length - IV_SIZE];
            System.arraycopy(encryptedData, IV_SIZE, data, 0, data.length);

            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            log.error("AES解密失败", e);
            throw e;
        }
    }

    @Override
    public byte[] encryptWithRsa(byte[] data, String publicKeyContent) throws GeneralSecurityException {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyContent);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            int maxEncryptSize = publicKey.getModulus().bitLength() / 8 - 11;
            if (data.length <= maxEncryptSize) {
                return cipher.doFinal(data);
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int offset = 0;
            while (offset < data.length) {
                int length = Math.min(maxEncryptSize, data.length - offset);
                byte[] encrypted = cipher.doFinal(data, offset, length);
                baos.write(encrypted);
                offset += length;
            }
            return baos.toByteArray();
        } catch (GeneralSecurityException | IOException e) {
            log.error("RSA加密失败", e);
            throw new GeneralSecurityException("RSA加密失败：" + e.getMessage(), e);
        }
    }

    @Override
    public byte[] decryptWithRsa(byte[] encryptedData, String privateKeyContent) throws GeneralSecurityException {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);

            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            int maxDecryptSize = privateKey.getModulus().bitLength() / 8;
            if (encryptedData.length <= maxDecryptSize) {
                return cipher.doFinal(encryptedData);
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int offset = 0;
            while (offset < encryptedData.length) {
                int length = Math.min(maxDecryptSize, encryptedData.length - offset);
                byte[] decrypted = cipher.doFinal(encryptedData, offset, length);
                baos.write(decrypted);
                offset += length;
            }
            return baos.toByteArray();
        } catch (GeneralSecurityException | IOException e) {
            log.error("RSA解密失败", e);
            throw new GeneralSecurityException("RSA解密失败：" + e.getMessage(), e);
        }
    }

    @Override
    public DigitalEnvelopeDTO createDigitalEnvelope(byte[] data, Long deptId) throws GeneralSecurityException, IOException {
        if (data == null || data.length == 0) {
            throw new BusinessException("加密数据不能为空");
        }
        if (deptId == null) {
            throw new BusinessException("部门ID不能为空");
        }

        EncryptionKey publicKey = encryptionKeyMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EncryptionKey>()
                        .eq(EncryptionKey::getKeyType, "RSA_PUBLIC")
                        .eq(EncryptionKey::getDeptId, deptId)
                        .eq(EncryptionKey::getStatus, 1)
        );

        if (publicKey == null) {
            throw new BusinessException("未找到部门" + deptId + "的公钥");
        }

        byte[] aesKey = generateAesKey();
        byte[] encryptedData = encryptWithAes(data, aesKey);
        byte[] encryptedAesKey = encryptWithRsa(aesKey, publicKey.getKeyContent());

        DigitalEnvelopeDTO envelope = new DigitalEnvelopeDTO();
        envelope.setEncryptedData(Base64.getEncoder().encodeToString(encryptedData));
        envelope.setEncryptedAesKey(Base64.getEncoder().encodeToString(encryptedAesKey));

        log.info("数字信封创建成功，部门ID：{}，数据大小：{}字节", deptId, data.length);
        return envelope;
    }

    @Override
    public byte[] openDigitalEnvelope(DigitalEnvelopeDTO envelope, Long deptId) throws GeneralSecurityException, IOException {
        if (envelope == null || StrUtil.isBlank(envelope.getEncryptedData())
                || StrUtil.isBlank(envelope.getEncryptedAesKey())) {
            throw new BusinessException("数字信封数据不完整");
        }
        if (deptId == null) {
            throw new BusinessException("部门ID不能为空");
        }

        EncryptionKey privateKey = encryptionKeyMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EncryptionKey>()
                        .eq(EncryptionKey::getKeyType, "RSA_PRIVATE")
                        .eq(EncryptionKey::getDeptId, deptId)
                        .eq(EncryptionKey::getStatus, 1)
        );

        if (privateKey == null) {
            throw new BusinessException("未找到部门" + deptId + "的私钥，您无权解密此文件");
        }

        byte[] encryptedData = Base64.getDecoder().decode(envelope.getEncryptedData());
        byte[] encryptedAesKey = Base64.getDecoder().decode(envelope.getEncryptedAesKey());

        byte[] aesKey = decryptWithRsa(encryptedAesKey, privateKey.getKeyContent());
        byte[] originalData = decryptWithAes(encryptedData, aesKey);

        log.info("数字信封解密成功，部门ID：{}，数据大小：{}字节", deptId, originalData.length);
        return originalData;
    }
}
