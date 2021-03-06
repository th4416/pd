package cc.typedef.droid.common.mime;

import java.io.File;
import java.util.Locale;

import libcore.net.MimeUtils;
import cc.typedef.droid.common.R;

public class MimeTypeUtil {

    private enum Category {
        REGULAR_FILE, IMAGE, AUDIO, VIDEO;

        public static Category from(String s) {
            for (Category c : Category.values()) {
                if (c.name().equalsIgnoreCase(s)) {
                    return c;
                }
            }
            return REGULAR_FILE;
        }
    }

    public static final String MIME_DIRECTORY = "vnd.android.document/directory";

    private static Category getCategory(String mimeType) {
        String major = majorMimeType(mimeType);
        Category c = null;
        if (major != null) {
            c = Category.from(major);
            if (c == null) {
                c = Category.REGULAR_FILE;
            }
        }
        return c;
    }

    public static boolean isImage(String mimeType) {
        return getCategory(mimeType) == Category.IMAGE;
    }

    public static boolean isVideo(String mimeType) {
        return getCategory(mimeType) == Category.VIDEO;
    }

    private static String majorMimeType(String mimeType) {
        return splitMimeType(mimeType)[0];
    }

    /**
     * MIME, see http://en.wikipedia.org/wiki/MIME<br/>
     * <code>null</code> matches nothing<br/>
     */
    public static boolean matches(String acceptableMimeType, String test) {
        if (acceptableMimeType == null || test == null) {
            return false;
        }
        String[] a1 = splitMimeType(acceptableMimeType);
        String[] a2 = splitMimeType(test);
        return segMatches(a1[0], a2[0]) && segMatches(a1[1], a2[1]);
    }

    /**
     * <code>null</code> matches nothing<br/>
     */
    public static boolean matchesAny(String[] acceptables, String[] tests) {
        if (acceptables != null && tests != null) {
            for (String p : acceptables) {
                for (String s : tests) {
                    if (matches(p, s)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static String mimeTypeByFile(File file) {
        if (file.isDirectory()) {
            // return Document.MIME_TYPE_DIR;
            return MIME_DIRECTORY;
        }

        String path = file.getName();
        int i = path.lastIndexOf('.');
        if (i >= 0) {
            String ext = path.substring(i + 1).toLowerCase(Locale.ROOT);
            return MimeUtils.guessMimeTypeFromExtension(ext);
        }
        return null;
    }

    /**
     * return at least the resId of generic file<br/>
     */
    public static int resIdByMimeType(String mimeType) {
        if (mimeType.equalsIgnoreCase(MIME_DIRECTORY)) {
            return R.drawable.mime_type_d;
        }
        switch (getCategory(mimeType)) {
            case IMAGE:
                return R.drawable.mime_image;
            case AUDIO:
                return R.drawable.mime_audio;
            case VIDEO:
                return R.drawable.mime_video;
            default:
                return R.drawable.mime_type_f;
        }
    }

    /**
     * <code>null</code> matches nothing<br/>
     */
    private static boolean segMatches(String p, String s) {
        if (p == null) {
            return false;
        }
        return p.equals("*") || p.equalsIgnoreCase(s);
    }

    /**
     * "use strict";<br/>
     * return [null, nul] if illegal grammar<br/>
     * always return an array of two
     */
    private static String[] splitMimeType(String mimeType) {
        if (mimeType != null) {
            int i = mimeType.indexOf('/');
            if (i > 0 && i < mimeType.length() - 1) {
                return new String[] {
                        mimeType.substring(0, i),
                        mimeType.substring(i + 1)
                };
            }
        }
        return new String[] {
                null, null
        };
    }
}
