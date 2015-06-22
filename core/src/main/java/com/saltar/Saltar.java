package com.saltar;

import com.saltar.client.HttpClient;
import com.saltar.converter.Converter;
import com.saltar.http.Request;
import com.saltar.http.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class Saltar {

    private String serverUrl;
    private HttpClient client;
    private Executor executor;
    private Executor callbackExecutor;
    private RequestInterceptor requestInterceptor;
    private Converter converter;

    private final Map<Class, ActionHelper> actionHelperCache = new HashMap<Class, ActionHelper>();
    private ActionHelperFactory actionHelperFactory;

    private Saltar(Builder builder) {
        this.serverUrl = builder.serverUrl;
        this.client = builder.client;
        this.executor = builder.executor;
        this.callbackExecutor = builder.callbackExecutor;
        this.requestInterceptor = builder.requestInterceptor;
        this.converter = builder.converter;
        loadActionHelperFactory();
    }

    private void loadActionHelperFactory() {
        try {
            Class<? extends ActionHelperFactory> clazz
                    = (Class<? extends ActionHelperFactory>) Class.forName("com.saltar.ActionHelperFactoryImpl");
            actionHelperFactory = clazz.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("It failed to create the Saltar");//TODO: add sаltar exception
        }
    }

    public <A> A executeAction(A action) {
        ActionHelper<A> helper = getActionHelper(action.getClass());
        Request request = helper.createRequest(action, new RequestBuilder(serverUrl, converter));
        Response response = invokeRequest(request);
        action = helper.fillResponse(action, response, converter);
        return action;
    }

    public <A> void executeAction(final A action, Callback<A> callback) {
        executor.execute(new CallbackRunnable<A>(action, callback, callbackExecutor) {
            @Override
            protected void executeAction(A action) {
                executeAction(action);
            }
        });
    }

    private ActionHelper getActionHelper(Class actionClass) {
        ActionHelper helper = actionHelperCache.get(actionClass);
        if (helper == null) {
            synchronized (this) {
                helper = actionHelperFactory.make(actionClass);
                actionHelperCache.put(actionClass, helper);
            }
        }
        return helper;
    }

    private Response invokeRequest(Request request) {
        try {
            Response response = client.execute(request);
            return response;
        } catch (IOException e) {
            throw new RuntimeException("invoke request exception");//TODO: add sаltar exception
        }
    }

    public static interface ActionHelper<T> {
        Request createRequest(T action, RequestBuilder requestBuilder);

        T fillResponse(T action, Response response, Converter converter);
    }

    static interface ActionHelperFactory {
        ActionHelper make(Class actionClass);
    }

    /**
     * Intercept every request before it is executed in order to add additional data.
     */
    public static interface RequestInterceptor {
        /**
         * Called for every request. Add data using methods on the supplied {@link RequestFacade}.
         */
        void intercept(Request request);


        /**
         * A {@link RequestInterceptor} which does no modification of requests.
         */
        RequestInterceptor NONE = new RequestInterceptor() {
            @Override
            public void intercept(Request request) {
                // Do nothing.
            }
        };
    }

    private static abstract class CallbackRunnable<A> implements Runnable {
        private final Callback<A> callback;
        private final Executor callbackExecutor;
        private final A action;

        private CallbackRunnable(A action, Callback<A> callback, Executor callbackExecutor) {
            this.action = action;
            this.callback = callback;
            this.callbackExecutor = callbackExecutor;
        }

        @Override
        public final void run() {
            try {
                executeAction(action);
                callbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(action);
                    }
                });
            } catch (final Exception e) {
                callbackExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFail(e);
                    }
                });
            }
        }

        protected abstract void executeAction(A action);
    }


    public static class Builder {
        private String serverUrl;
        private HttpClient client;
        private Executor executor;
        private Executor callbackExecutor;
        private RequestInterceptor requestInterceptor;
        private Converter converter;

        /**
         * API URL.
         */
        public Builder setServerUrl(String serverUrl) {
            if (serverUrl == null || serverUrl.trim().length() == 0) {
                throw new NullPointerException("Endpoint may not be blank.");
            }
            this.serverUrl = serverUrl;
            return this;
        }

        /**
         * The HTTP client used for requests.
         */
        public Builder setClient(HttpClient client) {
            if (client == null) {
                throw new NullPointerException("Client provider may not be null.");
            }
            this.client = client;
            return this;
        }

        /**
         * Executors used for asynchronous HTTP client downloads and callbacks.
         *
         * @param httpExecutor Executor on which HTTP client calls will be made.
         */
        public Builder setExecutor(Executor httpExecutor) {
            if (httpExecutor == null) {
                throw new NullPointerException("HTTP executor may not be null.");
            }
            this.executor = httpExecutor;
            return this;
        }

        public Builder setCallbackExecutor(Executor callbackExecutor) {
            if (callbackExecutor == null) {
                throw new NullPointerException("HTTP executor may not be null.");
            }
            this.callbackExecutor = callbackExecutor;
            return this;
        }

        /**
         * A request interceptor for adding data to every request.
         */
        public Builder setRequestInterceptor(RequestInterceptor requestInterceptor) {
            if (requestInterceptor == null) {
                throw new NullPointerException("Request interceptor may not be null.");
            }
            this.requestInterceptor = requestInterceptor;
            return this;
        }

        /**
         * The converter used for serialization and deserialization of objects.
         */
        public Builder setConverter(Converter converter) {
            if (converter == null) {
                throw new NullPointerException("Converter may not be null.");
            }
            this.converter = converter;
            return this;
        }

        /**
         * Create the {@link Saltar} instance.
         */
        public Saltar build() {
            if (serverUrl == null) {
                throw new IllegalArgumentException("Endpoint may not be null.");
            }
            fillDefaults();
            return new Saltar(this);
        }

        private void fillDefaults() {
            if (converter == null) {
                converter = Defaults.getConverter();
            }
            if (client == null) {
                client = Defaults.getClient();
            }
            if (executor == null) {
                executor = Defaults.getDefaultHttpExecutor();
            }
            if (converter == null) {
                converter = Defaults.getConverter();
            }
            if (callbackExecutor == null) {
                callbackExecutor = Defaults.defaultCallbackExecutor();
            }
            if (requestInterceptor == null) {
                requestInterceptor = RequestInterceptor.NONE;
            }
        }
    }
}