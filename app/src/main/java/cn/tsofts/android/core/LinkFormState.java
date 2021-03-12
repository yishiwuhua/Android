package cn.tsofts.android.core;

public class LinkFormState {
    private Integer urlError = null;
    private final boolean isDataValid;

	//杀杀杀杀杀杀杀杀杀
    LinkFormState(Integer urlError) {
        this.urlError = urlError;
        this.isDataValid = false;
    }

    LinkFormState(boolean isDataValid) {
        this.isDataValid = isDataValid;
    }

    Integer getUrlError() {
        return urlError;
    }

    boolean isDataValid() {
        return isDataValid;
    }
}
