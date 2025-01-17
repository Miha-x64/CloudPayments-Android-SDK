# CloudPayments SDK for Android 

## This fork
* no Gson dependency
* no Jsoup dependency
* The dialog can be used separately, without Fragment (e.g. with Conductor Controller)
* No required dependency on Volley.
  You're free to choose whether to use Volley, OkHttp, or neither, and don't ever request bin-code data
* Fragment result API support

```groovy
// root
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}

// app
dependencies {
    implementation 'com.github.Miha-x64:CloudPayments-Android-SDK:-SNAPSHOT' // or e9b4c2515a commit, for example
}
```
____________________

# Поддержка этой версии SDK закончена 31.12.2021, эта версия больше не будет обновляться, рекомендуем перейти на новую версию: https://github.com/cloudpayments/CloudPayments-SDK-Android

CloudPayments SDK позволяет интегрировать прием платежей в мобильные приложение для платформы Android.

### Схема работы мобильного приложения:
![Схема проведения платежа](https://cloudpayments.ru/storage/SNbUKmXtE1XgZoL7ypOSJBTFKvRpfMaWtWiNI51U.png)
1. В приложении необходимо получить данные карты: номер, срок действия, имя держателя и CVV;
2. Создать криптограмму карточных данных при помощи SDK;
3. Отправить криптограмму и все данные для платежа с мобильного устройства на ваш сервер;
4. С сервера выполнить оплату через платежное API CloudPayments. 

### Требования
Для работы CloudPayments SDK необходим Android версии 4.0.3 (API level 15) и выше.

### Добавление SDK в ваш проект
Для подключения CloudPayments SDK добавьте в файл build.gradle вашего проекта следующую зависимость:

```
implementation 'ru.cloudpayments.android:sdk:1.0.8'
```
### Структура проекта:

* **api** - Пример файлов для проведения платежа через ваш сервер
* **app** - Пример реализации приложения с использованием SDK
* **sdk** - Исходный код SDK


### Подготовка к работе

Для начала приема платежей через мобильные приложения вам понадобятся:

* Public ID;
* Пароль для API (**Важно:** Не храните пароль для API в приложении, выполняйте запросы через сервер согласно Схемы работы мобильного приложения).

Эти данные можно получить в личном кабинете: [https://merchant.cloudpayments.ru/](https://merchant.cloudpayments.ru/) после подключения к [CloudPayments](https://cloudpayments.ru/).

### Возможности CloudPayments SDK:

* Проверка карточного номера на корректность

```
boolean CPCard.isValidNumber(String cardNumber);

```

* Проверка срока действия карты

```
boolean CPCard.isValidExpDate(String cardDate); // cardDate в формате MMYY

```

* Определение типа платежной системы

```
CPCard card = new CPCard(String cardNumber, String cardDate, String cardCVC);
String card.getType();

```

* Определение банка эмитента

```
CPCardApi api = new CPCardApi(this);

        // Пример определения банка по номеру карты
        api.getBinInfo(cardNumber, binInfo -> {
            binInfo.getBankName(); // Название банка
            binInfo.getLogoUrl(); // URL картинки логотипа банка
        }, message -> {
            Log.e("Error", message);
        });

```

* Шифрование карточных данных и создание криптограммы для отправки на сервер

```
CPCard card = new CPCard(String cardNumber, String cardDate, String cardCVC);
String card.cardCryptogram(String publicId);

```

* Отображение 3DS формы и получении результата 3DS аутентификации

```
ThreeDsDialogFragment.newInstance(transaction.getAcsUrl(),
                String transactionId,
                String paReq)
                .show(getFragmentManager(), "3DS");
```

### Пример проведения платежа:

#### 1) Создание криптограммы

```
// Обязательно проверяйте входящие данные карты (номер, срок действия и cvc код) на корректность, иначе при попытке создания объекта CPCard мы получим исключение.
CPCard card = new CPCard(String cardNumber, String cardDate, String cardCVC);
String card.cardCryptogram(String publicId);

```

#### 2) Выполнение запроса на проведения платежа через  API CloudPayments

Платёж - [оплата по криптограмме](https://developers.cloudpayments.ru/#oplata-po-kriptogramme).

Для привязки карты (платёж "в один клик")  используйте метод
[оплату по токену](https://developers.cloudpayments.ru/#oplata-po-tokenu-rekarring).  

Токен можно получить при совершении оплаты по криптограмме, либо при получении  [уведомлений](https://developers.cloudpayments.ru/#uvedomleniya).


#### 3) Если необходимо, показать 3DS форму для подтверждения платежа

```
ThreeDsDialogFragment.newInstance(transaction.getAcsUrl(),
                String transactionId,
                String paReq)
                .show(getFragmentManager(), "3DS");
```

Для получения результатов прохождения 3DS аутентификации реализуйте интерефейс ThreeDSDialogListener в Activity из которой происходит создание и отображение ThreeDsDialogFragment.

```
public class CheckoutActivity implements ThreeDSDialogListener {
...
  @Override
    public void onAuthorizationCompleted(String md, String paRes) {
        post3ds(md, paRes); // Успешное прохождение аутентификации, для завершения оплаты выполните запрос API post3ds
    }

    @Override
    public void onAuthorizationFailed(String html) {
        showToast("AuthorizationFailed"); // Неудалось пройти аутентификацию, отобразите ошибку.
    }
}
```

#### 4) Для завершения оплаты выполнить метод Post3ds

Смотрите документацию по API: Платёж - [обработка 3-D Secure](https://developers.cloudpayments.ru/#obrabotka-3-d-secure).

### Подключение Google Pay  через CloudPayments

[О Google Pay](https://cloudpayments.ru/wiki/integration/products/googlepay)

[Документация](https://developers.google.com/payments/setup)

[Пример использования Google Pay API от Google](https://github.com/android-pay/paymentsapi-quickstart)

#### Включение Google Pay 

В файл build.gradle подключите следующую зависимость:

```
implementation 'com.google.android.gms:play-services-wallet:16.0.1'
```

В файл манифест приложения добавьте мета информацию:

```
<meta-data
            android:name="com.google.android.gms.wallet.api.enabled"
            android:value="true" />
```

#### Проведение платежа через Google Pay  

Сконфигурируйте параметры:

```
PaymentMethodTokenizationParameters  params =
		PaymentMethodTokenizationParameters.newBuilder()
				.setPaymentMethodTokenizationType(
				WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
				.addParameter("gateway", "cloudpayments")
				.addParameter("gatewayMerchantId", "Ваш Public ID")
				.build();
```

Укажите тип оплаты через шлюз (Wallet-Constants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY) и добавьте два параметра:

1) gateway: cloudpayments

2) gatewayMerchantId: Ваш Public ID, его можно посмотреть в [личном кабинете](https://merchant.cloudpayments.ru/).

С этими параметрами запросите токен Google Pay:

```
String tokenGP = paymentData.getPaymentMethodToken().getToken();
```

Используя токен Google Pay в качестве криптограммы карточных данных, совершите платёж  методами API, указанными ранее.

**В случае проведения платежа с токеном Google Pay в качестве имени держателя карты неоходимо указать: "Google Pay"**

### Поддержка

По возникающим вопросам техничечкого характера обращайтесь на support@cloudpayments.ru
