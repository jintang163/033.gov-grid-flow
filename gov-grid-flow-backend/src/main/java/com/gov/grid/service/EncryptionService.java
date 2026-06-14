package com.gov.grid.service;

import com.gov.grid.dto.DigitalEnvelopeDTO;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface EncryptionService {

    byte[] encryptWithAes(byte[] data, byte[] aesKey) throws GeneralSecurityException;

    byte[] decryptWithAes(byte[] encryptedData, byte[] aesKey) throws GeneralSecurityException;

    byte[] encryptWithRsa(byte[] data, String publicKeyContent) throws GeneralSecurityException;

    byte[] decryptWithRsa(byte[] encryptedData, String privateKeyContent) throws GeneralSecurityException;

    DigitalEnvelopeDTO createDigitalEnvelope(byte[] data, Long deptId) throws GeneralSecurityException, IOException;

    byte[] openDigitalEnvelope(DigitalEnvelopeDTO envelope, Long deptId) throws GeneralSecurityException, IOException;

    byte[] generateAesKey();
}
