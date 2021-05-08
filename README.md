# OpenHAB 2 MegaD binding (ДОКУМЕНТАЦИЯ НЕ АКТУАЛЬНА ДЛЯ OPENHAB 3, постараюсь привести в порядок максимально быстро)

режимы работы: "in", "out", "dimmer", "temp", "humidity", "onewire", "adc", "at", "st", "ib", "tget", "contact", в процессе "i2c".

## как запустить? 

### 1) через Веб-интерфейс


### 2) через файлы

.things:

```
Bridge megad:tcp:megadeviceincoming [port=8989]
{
	Bridge  device  mega1  "Mega 1 hardware"  [hostname="192.168.0.14", password="sec"] {
		Thing standard  megaStandardPortFunc  "Mega port10" @ "Mega" [port="10", refresh="0"]
		Bridge itoc i2cbus              "MegaD I2C Bridge"           [port="30", scl="31"] {
			Thing i2cbussensor mLs   "MegaD P30 Датчик освещенности"  [sensortype="max44009", refresh="60"]
			Thing i2cbussensor mtemp   "MegaD P30 Temp"       [sensortype="htu21d", refresh="60"]
		}
	}
}
```


.items:

```

```


Принцип такой: 
#### 1) Создаем бридж в файле .things.

```
Bridge megad:tcp:megadeviceincoming [port=8989] {}
```

megad:tcp: - обязятельное поле, после двоеточия - произвольное название.
#### 2) Добавляем определение адреса меги внутрь фигурных скобок
```
Bridge megad:tcp:megadeviceincoming [port=8989]
{
	Bridge  device  mega1  "Mega 1 hardware"  [hostname="192.168.0.14", password="sec"] {
	}
}
```

device - обязательное поле, далее произвольное название

#### 2) Добавляем Thing (По сути наши порты для меги) внутрь фигурных скобок выбранной меги

```
Bridge megad:tcp:megadeviceincoming[port=8989]
{
	Bridge  device  mega1  "Mega 1 hardware"  [hostname="192.168.0.14", password="sec"] {
		Thing standard onewire "Датчик" [port="3", refresh="10"]
		Thing standard kitchenout "Выключатель" [port="1", refresh="0"]
		Thing standard bedroomcontact "Окно" [port="2", refresh="0"]
	}
}

```

standard - обязательное поле, далее произвольное название

#### 3) открываем .items и создаем наши переменные.

```
Number Temperature_GF_Corridor "Temperature [%.1f °C]" <temperature> (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:onewire:onewire" }
Switch MegaDBindingThing_Input "Temperature " (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:kitchenout:out" } 
Contact MegaDContact "[%s]" (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:bedroomcontact:contact" }
```

Последний параметр - режимы работы(каналы). до этого - путь, который мы создали в .things (megad:device:megadeviceincoming: - это название бриджа, bedroomcontact: - название Thing )


#### 4) Далее аналогично 1 версии опенхаба

## Как собрать?

1. Скачать: 
	[Java](https://jdk.java.net/12/)
	[Maven](https://maven.apache.org/download.cgi)

2. Загрузить архив со всеми плагинами [отсюда](https://github.com/openhab/openhab2-addons/archive/master.zip) и распаковать
3. Загрузить архив Мегад [отсюда](https://github.com/Pshatsillo/openhab2MegadBinding/archive/master.zip) и распаковать
2. Скопировать директорию `org.openhab.binding.megad` в папку `/openhab2-addons/bundles`.
3. Перейти в скопированную папку и выполнить `mvn clean install`. Сборка должна пройти успешно и в папке `target` появиться архив с байндингом:

```bash
org.openhab.binding.megad git:(master) ✗ ls -l target | grep megad
-rw-r--r--   1 xxxxxxx  staff  29482 10 мар 21:35 org.openhab.binding.megad-2.5.0-SNAPSHOT.jar
```

## Или скачать готовый jar файл [отсюда](https://github.com/Pshatsillo/openhab2MegadBinding/releases)

## Как что-нибудь исправить?

1. Пройти по этой ссылке https://www.openhab.org/docs/developer/ide/eclipse.html
2. После пункта 5 в Eclipse IDE Setup перейти в папку `/openhab2-addons/bundles` и выполнить `git submodule add  https://github.com/Pshatsillo/openhab2MegadBinding.git org.openhab.binding.megad` .
3. Отредактировать файл `openhab2-addons/bom/openhab-addons/pom.xml` следующим образом: 

```bash
<dependency>
    <groupId>org.openhab.addons.bundles</groupId>
    <artifactId>org.openhab.binding.megad</artifactId>
    <version>${project.version}</version>
</dependency> 
```

добавить эти строки в конец похожих записей

4. Перейти в папку `openhab2-addons\bom\openhab-addons` и запустить команду `mvn -DskipChecks -DskipTests clean install`

5. Импортировать проект org.openhab.binding.megad в Eclipse IDE

6. Отредактировать файл Eclipse `\launch\app\runtime\logback.xml`. Добавить в него эту строку: 

```bash
  <logger name="org.openhab.binding" level="DEBUG"/>
```

## Еще

По многочисленным просьбам - Donate:

[Paypal](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=P38VCVDQMSMYQ) 

[Yandex.Money](https://money.yandex.ru/to/410011024847033)

Спасибо!
