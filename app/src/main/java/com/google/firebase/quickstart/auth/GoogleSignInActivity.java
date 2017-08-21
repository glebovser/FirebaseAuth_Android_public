package com.google.firebase.quickstart.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * Activity с Гугл аутентификацией
 */
public class GoogleSignInActivity extends BaseActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    // Для логов
    private static final String TAG = "GoogleActivity";
    // Рандомное число для использования константы
    private static final int RC_SIGN_IN = 9001;

    // Переменная FirebaseAuth
    private FirebaseAuth mAuth;

    // Переменная для доступа к сервису Гугл
    private GoogleApiClient mGoogleApiClient;

    private TextView mStatusTextView;
    private TextView mDetailTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google);

        // Соединяем UI
        mStatusTextView = (TextView) findViewById(R.id.status);
        mDetailTextView = (TextView) findViewById(R.id.detail);

        // Кнопки
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);

        // Конфигурируем Гугл логин (стандартный  код)
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // Инициализируем FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
    }

    // Определяем действие при нажатии на кнопку
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.sign_in_button) {
            signIn();
        } else if (i == R.id.sign_out_button) {
            signOut();
        } else if (i == R.id.disconnect_button) {
            revokeAccess();
        }
    }

    // Определяем действие при нажатии на кнопку
    @Override
    public void onStart() {
        super.onStart();
        // Проверка, залогинен ли уже текущий пользователь
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    // Начинаем процесс проверки данных Гугл аккаунта
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // Определяем действия после проверки данных (успешной или нет)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Проверяем код (тот же, который мы определили когда запускали процесс проверки данных Гугл аккаунта)
        if (requestCode == RC_SIGN_IN) {
            // Смотрим результат
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Данные подтверждены , теперь аутентифицируемся (логинимся) с Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Проверка данных провалилась, даем об этом знать пользователю и выводим это в лог
                updateUI(null);
            }
        }
    }

    // Аутентифицируем пользователя по его Гугл аккаунту
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "Логин Гугл:" + acct.getId());
        // Показывает спиннер
        showProgressDialog();
        // Берем данные пользователя
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        // Логинимся с Firebase
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Логин успешен, обновляем пользовательский интерфейс, отображаем информацию аккаунта пользователя
                            Log.d(TAG, "Логин: успех");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // Если логин неудачный, отобрази сообщение пользователю
                            Log.w(TAG, "Логин: провал", task.getException());
                            Toast.makeText(GoogleSignInActivity.this, "Аутентификация провалилась.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        hideProgressDialog();
                    }
                });
    }

    // Логаут
    private void signOut() {
        // Логаут
        mAuth.signOut();

        // Гугл логаут
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateUI(null);
                    }
                });
    }

    // Дисконнект
    private void revokeAccess() {
        // Логаут
        mAuth.signOut();

        // Гугл дисконнект
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateUI(null);
                    }
                });
    }

    // Обновление пользовательского интерфейса
    private void updateUI(FirebaseUser user) {
        hideProgressDialog();
        if (user != null) {
            mStatusTextView.setText(getString(R.string.google_status_fmt, user.getEmail()));
            mDetailTextView.setText(getString(R.string.firebase_status_fmt, user.getUid()));

            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);
            mDetailTextView.setText(null);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    // Переписываем метод onConnectionFailed() на случай, если произошла ошибка со стороны Гугл
    // Информируем пользователя и выводим сообщение в лог
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Ошибка сервисов Гугл" + connectionResult);
        Toast.makeText(this, "Ошибка сервисов Гугл", Toast.LENGTH_SHORT).show();
    }
}
