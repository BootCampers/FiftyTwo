package org.bootcamp.fiftytwo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.parse.ParseAnonymousUtils;

import org.bootcamp.fiftytwo.R;
import org.bootcamp.fiftytwo.models.User;
import org.bootcamp.fiftytwo.utils.Constants;
import org.bootcamp.fiftytwo.utils.PlayerUtils;
import org.parceler.Parcels;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.bootcamp.fiftytwo.utils.AppUtils.loadRoundedImage;
import static org.bootcamp.fiftytwo.utils.Constants.PARAM_USER;
import static org.bootcamp.fiftytwo.utils.Constants.PICK_IMAGE_REQUEST;
import static org.bootcamp.fiftytwo.utils.Constants.SELECTED_AVATAR;
import static org.bootcamp.fiftytwo.utils.NetworkUtils.isNetworkAvailable;

public class RegisterActivity extends AppCompatActivity {

    private String userAvatarURI = "";

    @BindView(R.id.etUserName) EditText etUserName;
    @BindView(R.id.ivAvatar) ImageView ivAvatar;
    @BindView(R.id.fabBrowseAvatar) FloatingActionButton fabBrowseAvatar;
    @BindView(R.id.btnRegister) Button btnRegister;
    @BindView(R.id.scrollViewRegister) ScrollView scrollViewRegister;
    @BindView(R.id.networkFailureBanner) RelativeLayout networkFailureBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);

        if (User.getCurrentUser() != null) {
            startWithCurrentUser();
        } else {
            loginToParse();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (isNetworkAvailable(this)) {
            notifyNetworkFailure(false);

            User user = User.get(this);
            if (null != user) {
                Intent selectGameIntent = new Intent(RegisterActivity.this, SelectGameActivity.class);
                selectGameIntent.putExtra(PARAM_USER, Parcels.wrap(user));
                startActivity(selectGameIntent);
            } else {
                userAvatarURI = PlayerUtils.getDefaultAvatar();
                loadRoundedImage(this, ivAvatar, userAvatarURI);
            }
        } else {
            notifyNetworkFailure(true);
        }
    }

    @OnClick(R.id.fabBrowseAvatar)
    public void browse() {
        Intent intent = new Intent(RegisterActivity.this, AvatarSelectionActivity.class);
        startActivityForResult(intent, PICK_IMAGE_REQUEST, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            userAvatarURI = data.getStringExtra(SELECTED_AVATAR);
            Log.d(Constants.TAG, userAvatarURI);
            loadRoundedImage(this, ivAvatar, userAvatarURI);
        }
    }

    @OnClick(R.id.btnRegister)
    public void register() {
        String username = etUserName.getText().toString().replaceAll("\\s+", "");
        if (username.isEmpty()) {
            Toast.makeText(this, "Username must have a value!", Toast.LENGTH_SHORT).show();
            etUserName.requestFocus();
            scrollViewRegister.scrollTo(etUserName.getScrollX(), etUserName.getScrollY());
            return;
        }

        User user = new User(userAvatarURI, username, User.getCurrentUser().getObjectId());
        user.save(this);

        Intent selectGameIntent = new Intent(RegisterActivity.this, SelectGameActivity.class);
        selectGameIntent.putExtra(PARAM_USER, Parcels.wrap(user));
        finish();
        startActivity(selectGameIntent);
    }

    private void loginToParse() {
        ParseAnonymousUtils.logIn((user, e) -> {
            if (e != null) {
                Log.e("DEBUG", "Anonymous loginToParse failed: ", e);
            } else {
                startWithCurrentUser();
            }
        });
    }

    private void startWithCurrentUser() {
        // Do Nothing
    }

    private void notifyNetworkFailure(boolean networkFailure) {
        networkFailureBanner.setVisibility(networkFailure ? View.VISIBLE : View.GONE);
        fabBrowseAvatar.setEnabled(!networkFailure);
        btnRegister.setEnabled(!networkFailure);
    }
}