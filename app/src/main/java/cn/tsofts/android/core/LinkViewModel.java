package cn.tsofts.android.core;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LinkViewModel extends ViewModel {
    private MutableLiveData<LinkFormState> linkFormState = new MutableLiveData<>();
	//sssss5555
    LiveData<LinkFormState> getLinkFormState() {
        return linkFormState;
    }

    public void linkDataChanged(String url) {
        if (!isUrlValid(url)) {
            linkFormState.setValue(new LinkFormState(R.string.invalid_url));
        } else {
            linkFormState.setValue(new LinkFormState(true));
        }
    }

    private boolean isUrlValid(String url) {
        if (url == null) {
            return false;
        }
        return url.toLowerCase().startsWith("http");
    }
}
