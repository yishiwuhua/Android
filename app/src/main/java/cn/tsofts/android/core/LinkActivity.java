package cn.tsofts.android.core;
/**
惊雷无声
2004年，第一次用QQ，起的昵称，那个时候，专门用昵称查找，没有人用过这个昵称，所以选择了这个，现在搜索了一下，一大片……
惺惺杀杀杀惺惺水水水水我问问
*/
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.tencent.smtt.sdk.QbSdk;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class LinkActivity extends AppCompatActivity {
    private LinkViewModel linkViewModel;
    private String url;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link);

        //申请权限ccc
        if (ContextCompat.checkSelfPermission(LinkActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(LinkActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean arg0) {
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                Log.d("app", " onViewInitFinished is " + arg0);
            }

            @Override
            public void onCoreInitFinished() {
            }
        };
        //x5内核初始化接口
        QbSdk.initX5Environment(getApplicationContext(), cb);

        linkViewModel = new ViewModelProvider(this).get(LinkViewModel.class);
        final EditText urlEditText = findViewById(R.id.url);
        final Button linkButton = findViewById(R.id.link);
        final Switch remember = findViewById(R.id.remember);


        SharedPreferences info = getPreferences(Activity.MODE_PRIVATE);
        url = info.getString("URL", "");
        if (!url.equals("")) {
            urlEditText.setText(url);
            linkViewModel.linkDataChanged(urlEditText.getText().toString());
        }
        String rememberValue = info.getString("Remember", "0");
        if ("1".equals(rememberValue)) {
            remember.setChecked(true);
            if (!url.equals("")) {
                Intent intent = new Intent(LinkActivity.this, getWebActivity());
                intent.putExtra("URL", url);
                startActivity(intent);
            }
        }

        linkViewModel.getLinkFormState().observe(this, new Observer<LinkFormState>() {
            @Override
            public void onChanged(LinkFormState linkFormState) {
                if (linkFormState == null) {
                    return;
                }
                linkButton.setEnabled(linkFormState.isDataValid());
                if (linkFormState.getUrlError() != null) {
                    urlEditText.setError(getString(linkFormState.getUrlError()));
                }
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                linkViewModel.linkDataChanged(urlEditText.getText().toString());
            }
        };
        urlEditText.addTextChangedListener(afterTextChangedListener);
        remember.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences info = getPreferences(Activity.MODE_PRIVATE);
                info.edit().putString("Remember", isChecked ? "1" : "0").apply();
            }
        });

        linkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                url = urlEditText.getText().toString();
                SharedPreferences info = getPreferences(Activity.MODE_PRIVATE);
                info.edit().putString("URL", url).apply();
                Intent intent = new Intent(LinkActivity.this, getWebActivity());
                intent.putExtra("URL", url);
                startActivity(intent);
            }
        });
    }

    protected Class getWebActivity() {
        return WebActivity.class;
    }
}