package de.xikolo.utils;

public class ItemTitleUtil {

    public static String format(String moduleTitle, String itemTitle) {
        String title;
        if (itemTitle.length() > moduleTitle.length() + 2 &&
                itemTitle.startsWith(moduleTitle) &&
                itemTitle.substring(moduleTitle.length() + 1, moduleTitle.length() + 2).equals(" ")) {
            title = itemTitle.substring(moduleTitle.length() + 2);
        } else {
            title = itemTitle;
        }
        return title;
    }

}
