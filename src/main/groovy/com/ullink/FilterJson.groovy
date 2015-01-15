package com.ullink

public class FilterJson {
    StringWriter stringWriter = new StringWriter();
    boolean isAccepted = false;

    public  FilterJson(InputStream inputStream) {
        inputStream.filterLine(stringWriter, {
            if (!isAccepted && it.startsWith('{')) {
                isAccepted = true;
            }
            return isAccepted })
    }

    public String toString() {
        stringWriter.toString();
    }
}
