package com.example.servicedemo.restapi;

import androidx.annotation.Nullable;

import com.example.servicedemo.utils.URLS;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder().baseUrl(URLS.BASE_URL)
                    .addConverterFactory(StringConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .client(getUnsafeOkHttpClient()).build();
        }
        return retrofit;
    }

    private static OkHttpClient getUnsafeOkHttpClient() {

        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[0];
                }
            }};

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .connectTimeout(2, TimeUnit.MINUTES)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);
            httpClient.addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("Content-Type", "application/json")
                            .method(original.method(), original.body())
                            .build();

                    return chain.proceed(request);
                }
            });

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts,
                    new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext
                    .getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient();
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            okHttpClient = okHttpClient.newBuilder()
                    .sslSocketFactory(sslSocketFactory)
                    .hostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER).addInterceptor(logging).build();

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    public static class StringConverterFactory extends Converter.Factory {
        private final MediaType MEDIA_TYPE = MediaType.parse("text/plain");

        public static StringConverterFactory create() {
            return new StringConverterFactory();
        }

        @Nullable
        @Override
        public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
            if(String.class.equals(type)) {
                return new Converter<String, RequestBody>() {
                    @Override
                    public RequestBody convert(String value) throws IOException {
                        return RequestBody.create(MEDIA_TYPE, value);
                    }
                };
            }

            return null;
        }

        @Nullable
        @Override
        public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
            if (String.class.equals(type)) {
                return new Converter<ResponseBody, String>() {

                    @Override
                    public String convert(ResponseBody value) throws IOException {
                        return value.string();
                    }
                };
            }
            return null;
        }
    }
}
