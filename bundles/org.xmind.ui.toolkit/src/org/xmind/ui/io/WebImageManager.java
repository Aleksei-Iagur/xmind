package org.xmind.ui.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.xmind.ui.internal.ToolkitPlugin;

/**
 * 
 * @author Shawn Liu
 * @since 3.6.50
 */
public class WebImageManager {

    public static interface WebImageCallback {

        void handleWith(ImageDescriptor image);
    }

    public static interface WebImageCallback2 {

        void handleWith(String imagePath);
    }

    private static final Map<String, String> mime = new HashMap<String, String>();

    static {
        mime.put("image/bmp", ".bmp"); //$NON-NLS-1$ //$NON-NLS-2$
        mime.put("image/gif", ".gif"); //$NON-NLS-1$ //$NON-NLS-2$
        mime.put("image/x-icon", ".ico"); //$NON-NLS-1$ //$NON-NLS-2$
        mime.put("image/jpeg", ".jpg"); //$NON-NLS-1$ //$NON-NLS-2$
        mime.put("image/png", ".png"); //$NON-NLS-1$ //$NON-NLS-2$
        mime.put("image/tiff", ".tiff"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private static final int ID_LENGTH = 26;

    private static final char PADDING_CHAR = '0';

    private static final Random random = new Random(System.currentTimeMillis());

    private static WebImageManager webImageManager = null;

    private WebImageManager() {
    }

    public void requestWebImage(final String imageUrl,
            final WebImageCallback callback) {
        if (callback == null) {
            return;
        }

        if (imageUrl == null) {
            callback.handleWith(null);
            return;
        }

        //get image from web
        Job job = new Job("") { //$NON-NLS-1$

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask(null, 100);
                String imageFilePath = createWebImageFile(imageUrl);

                if (imageFilePath == null || imageFilePath.equals("")) { //$NON-NLS-1$
                    callback.handleWith(null);
                    return Status.OK_STATUS;
                }

                Image image_0 = new Image(Display.getCurrent(), imageFilePath);
                new File(imageFilePath).delete();

                ImageDescriptor imageDesc = null;
                if (image_0 != null) {
                    imageDesc = ImageDescriptor.createFromImage(image_0);
                }
                callback.handleWith(imageDesc);

                monitor.worked(100);
                return Status.OK_STATUS;
            }
        };

        job.setSystem(true);
        job.setUser(false);
        job.schedule();
    }

    public void requestWebImage(final String imageUrl,
            final WebImageCallback2 callback) {
        if (callback == null) {
            return;
        }

        if (imageUrl == null) {
            callback.handleWith(null);
            return;
        }

        //get image from web
        Job job = new Job("") { //$NON-NLS-1$

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask(null, 100);
                String imageFilePath = createWebImageFile(imageUrl);

                callback.handleWith(imageFilePath);

                monitor.worked(100);
                return Status.OK_STATUS;
            }
        };

        job.setSystem(true);
        job.setUser(false);
        job.schedule();
    }

    private String createWebImageFile(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }

        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        String tempImageFilePath = null;
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl)
                    .openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            String contentType = connection.getHeaderField("Content-Type"); //$NON-NLS-1$
            if (contentType == null) {
                return null;
            }

            String imageExtension = getImageExtension(contentType);
            if (imageExtension == null) {
                return null;
            }

            //write image from stream to file
            inputStream = connection.getInputStream();

            tempImageFilePath = getTempImageFilePath(imageUrl, imageExtension);
            File file = new File(tempImageFilePath);
            WebImageManager.ensureFileParent(file);

            outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int num = 0;
            while ((num = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, num);
                outputStream.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return tempImageFilePath;
    }

    private String getTempImageFilePath(String imageUrl,
            String imageExtension) {
        String tempFile = ToolkitPlugin.getDefault().getStateLocation()
                .append("/webImages/" //$NON-NLS-1$
                        + createId() + imageExtension)
                .toString();
        return tempFile;
    }

    private String createId() {
        BigInteger bi = new BigInteger(128, random);
        String id = bi.toString(32);
        int paddingLength = ID_LENGTH - id.length();
        if (paddingLength > 0) {
            StringBuffer buf = new StringBuffer(ID_LENGTH);
            for (int i = 0; i < paddingLength; i++) {
                buf.append(PADDING_CHAR);
            }
            buf.append(id);
            return buf.toString();
        }
        return id;
    }

    private static File ensureFileParent(File f) {
        ensureDirectory(f.getParentFile());
        return f;
    }

    private static File ensureDirectory(File dir) {
        if (!dir.exists())
            dir.mkdirs();
        return dir;
    }

    private static String getImageExtension(String contentType) {
        if (contentType == null) {
            return null;
        }
        return mime.get(contentType);
    }

    public static synchronized WebImageManager getInstance() {
        if (webImageManager == null) {
            webImageManager = new WebImageManager();
        }
        return webImageManager;
    }

}
