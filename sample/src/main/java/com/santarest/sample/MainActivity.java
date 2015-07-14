package com.santarest.sample;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import com.santarest.RequestBuilder;
import com.santarest.SantaRest;
import com.santarest.http.Request;
import com.santarest.http.Response;
import com.squareup.otto.Subscribe;

public class MainActivity extends ActionBarActivity {

    private SantaRest santaRest;
    private static final String AUTH_URL = "https://github.com/login/oauth/authorize?";
    String redirectUrl = "https://api.github.com";
    String clientId = "VladSumtsov";
    private static final String TOKEN_URL = "https://github.com/login/oauth/access_token?";
    private static final String API_URL = "https://api.github.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        santaRest = new SantaRest.Builder()
                .setServerUrl("https://api.github.com")
                .setRequestInterceptor(new SantaRest.RequestInterceptor() {
                    @Override
                    public void intercept(RequestBuilder request) {
                        request.addHeader("test", "test");
                        request.addHeader("Accept", "application/vnd.github.v3+json");
                    }
                })
                .addResponseInterceptors(new SantaRest.ResponseListener() {
                    @Override
                    public void onResponseReceived(Object action, Request request, Response response) {
                        System.out.println(request);
                        System.out.println(response);
                    }

                })
                .build();

        String url = AUTH_URL + "client_id=" + clientId + "&redirect_uri=" + redirectUrl;
        final GithubDialog dialog = new GithubDialog(MainActivity.this, url, redirectUrl, new GithubDialog.OAuthDialogListener() {
            @Override
            public void onComplete(String accessToken) {
                System.out.println("accessToken = " + accessToken);
            }

            @Override
            public void onError(String error) {
                System.out.println("error = " + error);
            }
        });
        findViewById(R.id.authenticate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });
    }

    @Subscribe
    public void onExampleAction(ExampleAction action) {
        System.out.println(action);
        System.out.println(action.success);
        System.out.println(action.isSuccess());
    }

    @Override
    protected void onResume() {
        super.onResume();
        santaRest.subscribe(this);
        santaRest.sendAction(new ExampleAction("square", "retrofit"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        santaRest.unsubscribe(this);
    }
}