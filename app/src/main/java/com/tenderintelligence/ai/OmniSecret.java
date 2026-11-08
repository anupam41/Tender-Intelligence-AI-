package com.tenderintelligence.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class OmniSecret {
 private static final String ALIAS="TenderRadarOmniRouteGatewayKey";
 private static final String PREF="omniroute_secure";
 private OmniSecret(){}
 private static SecretKey key() throws Exception {
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(store.containsAlias(ALIAS))return ((KeyStore.SecretKeyEntry)store.getEntry(ALIAS,null)).getSecretKey();
  KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
  return gen.generateKey();
 }
 static void save(Context context,String value) throws Exception {
  if(value==null||value.isEmpty()){clear(context);return;}
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
  byte[] encrypted=cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).putString("data",Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply();
 }
 static String load(Context context) {
  try {
   SharedPreferences p=context.getSharedPreferences(PREF,Context.MODE_PRIVATE);
   String iv=p.getString("iv",""),data=p.getString("data","");
   if(iv.isEmpty()||data.isEmpty())return "";
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));
   return new String(cipher.doFinal(Base64.decode(data,Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8);
  }catch(Exception e){return "";}
 }
 static void clear(Context context) {
  context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().clear().apply();
 }
}
