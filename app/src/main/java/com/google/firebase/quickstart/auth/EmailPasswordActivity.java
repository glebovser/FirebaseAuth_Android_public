package com.google.firebase.quickstart.auth;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Activity, которая регулирует логин/регистрацию по email
 */
public class EmailPasswordActivity extends BaseActivity implements
        View.OnClickListener {

    private static final String TAG = "EmailPassword";

    // UI переменные
    private TextView mStatusTextView;
    private TextView mDetailTextView;
    private EditText mEmailField;
    private EditText mPasswordField;
    // Переменная FirebaseAuth
    private FirebaseAuth mAuth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emailpassword);

        // Соединяем UI
        mStatusTextView = (TextView) findViewById(R.id.status);
        mDetailTextView = (TextView) findViewById(R.id.detail);
        mEmailField = (EditText) findViewById(R.id.field_email);
        mPasswordField = (EditText) findViewById(R.id.field_password);

        // Кнопки
        findViewById(R.id.email_sign_in_button).setOnClickListener(this);
        findViewById(R.id.email_create_account_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.verify_email_button).setOnClickListener(this);

        // инициализация FirebaseAuth
        mAuth = FirebaseAuth.getInstance();
    }

    // Проверка, залогинен ли уже текущий пользователь
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "Создание аккаунта :" + email);
        if (!validateForm()) {
            return;
        }

        showProgressDialog();

        // Создание аккаунта
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Успешно, обновляем пользовательский интерфейс
                            Log.d(TAG, "Создание аккаунта: успех");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // Провал, отобрази сообщение пользователю
                            Log.w(TAG, "Создание аккаунта: провал", task.getException());
                            Toast.makeText(EmailPasswordActivity.this, "Аутентификация не удалась",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        hideProgressDialog();
                    }
                });
    }
    // Логин
    private void signIn(String email, String password) {
        Log.d(TAG, "Логин: " + email);
        if (!validateForm()) {
            return;
        }

        showProgressDialog();

        // Логин с email
        mAuth.signInWithEmailAndPassword(email, password)
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
                            Toast.makeText(EmailPasswordActivity.this, "Аутентификация не удалась.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        if (!task.isSuccessful()) {
                            mStatusTextView.setText(R.string.auth_failed);
                        }
                        hideProgressDialog();
                    }
                });
    }
    // Логаут (выход)
    private void signOut() {
        mAuth.signOut();
        updateUI(null);
    }

    private void sendEmailVerification() {
        // Блокируем кнопку
        findViewById(R.id.verify_email_button).setEnabled(false);

        // Отправляем подтверждающий email
        final FirebaseUser user = mAuth.getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Разблокируем кнопку
                        findViewById(R.id.verify_email_button).setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(EmailPasswordActivity.this,
                                    "Подтверждающий email отправлен на " + user.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "отправление подтверждающего email", task.getException());
                            Toast.makeText(EmailPasswordActivity.this,
                                    "Ошибка",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    // Проверка формы
    private boolean validateForm() {
        boolean valid = true;

        String email = mEmailField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailField.setError("Обязательно у заполнению");
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Обязательно у заполнению");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    }

    // обновление пользовательского интерфейся
    private void updateUI(FirebaseUser user) {
        hideProgressDialog();
        if (user != null) {
            mStatusTextView.setText(getString(R.string.emailpassword_status_fmt,
                    user.getEmail(), user.isEmailVerified()));
            mDetailTextView.setText(getString(R.string.firebase_status_fmt, user.getUid()));

            findViewById(R.id.email_password_buttons).setVisibility(View.GONE);
            findViewById(R.id.email_password_fields).setVisibility(View.GONE);
            findViewById(R.id.signed_in_buttons).setVisibility(View.VISIBLE);

            findViewById(R.id.verify_email_button).setEnabled(!user.isEmailVerified());
        } else {
            mStatusTextView.setText(R.string.signed_out);
            mDetailTextView.setText(null);

            findViewById(R.id.email_password_buttons).setVisibility(View.VISIBLE);
            findViewById(R.id.email_password_fields).setVisibility(View.VISIBLE);
            findViewById(R.id.signed_in_buttons).setVisibility(View.GONE);
        }
    }

    // Распределение действий слушателя
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.email_create_account_button) {
            createAccount(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.email_sign_in_button) {
            signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.sign_out_button) {
            signOut();
        } else if (i == R.id.verify_email_button) {
            sendEmailVerification();
        }
    }
}
