package cn.jiajixin.nuwa.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.security.auth.x500.X500Principal;

/**
 * security check
 */
public class SecurityChecker {
    private static final String TAG = "SecurityChecker";

    private static final String CLASSES_DEX = "classes.dex";

    private static final X500Principal DEBUG_DN = new X500Principal(
            "CN=Android Debug,O=Android,C=US");

    /**
     * host publickey
     */
    private PublicKey mPublicKey;
    /**
     * host debuggable
     */
    private boolean mDebuggable;


    public SecurityChecker(Context context) {
        init(context);
    }

    /**
     * @param path Apk file
     * @return true if verify apk success
     */
    public boolean verifyApk(File path) {
        if (mDebuggable) {
            Log.d(TAG, "mDebuggable = true");
            return true;
        }

        JarFile jarFile = null;
        try {
            jarFile = new JarFile(path);

            JarEntry jarEntry = jarFile.getJarEntry(CLASSES_DEX);
            if (null == jarEntry) {// no code
                return false;
            }
            loadDigestes(jarFile, jarEntry);
            Certificate[] certs = jarEntry.getCertificates();
            if (certs == null) {
                return false;
            }
            return check(path, certs);
        } catch (IOException e) {
            Log.e(TAG, path.getAbsolutePath(), e);
            return false;
        } finally {
            try {
                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (IOException e) {
                Log.e(TAG, path.getAbsolutePath(), e);
            }
        }
    }

    private void loadDigestes(JarFile jarFile, JarEntry je) throws IOException {
        InputStream is = null;
        try {
            is = jarFile.getInputStream(je);
            byte[] bytes = new byte[8192];
            while (is.read(bytes) > 0) {
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private boolean check(File path, Certificate[] certs) {
        if (certs.length > 0) {
            for (int i = certs.length - 1; i >= 0; i--) {
                try {
                    certs[i].verify(mPublicKey);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, path.getAbsolutePath(), e);
                }
            }
        }
        return false;
    }


    private void init(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();

            PackageInfo packageInfo = pm.getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);
            CertificateFactory certFactory = CertificateFactory
                    .getInstance("X.509");
            ByteArrayInputStream stream = new ByteArrayInputStream(
                    packageInfo.signatures[0].toByteArray());
            X509Certificate cert = (X509Certificate) certFactory
                    .generateCertificate(stream);
            mDebuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
            mPublicKey = cert.getPublicKey();
        } catch (NameNotFoundException e) {
            Log.e(TAG, "init", e);
        } catch (CertificateException e) {
            Log.e(TAG, "init", e);
        }
    }

}