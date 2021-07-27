
# OpenHAB 3 MegaD binding

OpenHAB 3 MegaD binding создан для интеграции многофункционального контроллера [MegaD-2561](https://www.ab-log.ru/smart-house/ethernet/megad-2561 " ") в систему умного дома OpenHAB.

С помощью данного биндига реализуются приём и отправка GET-запросов на исполнительные устройства

Чтобы установить биндинг на сервер, потребуется [скачать](https://github.com/Pshatsillo/openhab2MegadBinding/releases)  файл биндинга и положить его в папку **Addons** сервера Openhab

В некоторых случаях может потребоваться выполнение команды через [консоль](https://www.openhab.org/docs/administration/console.html) команды `feature install openhab-transport-http`

В результате в веб-интерфейсе администрирования Openhab (Настройки->Things-> +)  должен появиться MegaD Binding

Далее есть два варианта настройки - текстовые файлы или веб-интерфейс

1. Веб-интерфейс:
	1. Настраиваем **Bridge for incoming connections**. Здесь нужно выбрать порт для входящих соединений от устройства на сервер
	1. Настраиваем **Bridge Megad hardware**. Нужно указать ip адрес и пароль для доступа к устройству. Обращаю внимание, что данный бридж зависит от предыдущего и его нужно обязательно указать в настройках.
	1. Далее возможны варианты настроек в зависимости от необходимиого функционала. 
	** MegaD Standard Thing** содержит в себе базовый функционал MegaD-2561, такой как вход, выход, диммер, температура с внешнего датчика Onewire, подключенного **напрямую в порт**
	**MegaD I2C Sensors** содержит в себе функционал для вывода информации с датчиков, подключенных по I2C **напрямую в порт**
	**MegaD Group thing** управляет **созданными на устройстве группами** (GET-запрос на устройство вида `g7:1`)
	**MegaD thing for encoder** реализует функционал управления устройством с помощью энкодера. В данный момент находится в тестовом виде
	**MegaD I2C LCD1609 display** реализует функционал по выводу информации на двустрочный дисплей LCD1609
	**MegaD rs485 Thing** реализует функционал по выводу информации по протоколу rs485. В данный момент реализован вывод со счётчика электроэнергии Eastron SDM 120
	1. Кроме того, реализована возможность вывода работы с устройствами, подключенными шиной.
		- **Bridge Megad 1 wire bus port** реализует опрос 1-wire датчиков, подключенных шиной, а **MegaD 1wire bus Thing** отвечает за обработку и вывод данных по выбранному датчику и зависит от **Bridge Megad 1 wire bus port**
		- **Bridge Megad I2C bus port** реализует опрос шины i2c и служит мостом для **MegaD I2C bus sensor handler**
		- **Bridge for Megad MCP23008/MCP23017 extenders** реализует поддержку расширителя [MCP23008/MCP23017](https://www.ab-log.ru/smart-house/ethernet/megad-2561#conf-exp-mcp) и является мостом для **MegaD MCP23008/MCP23017 extender port selector Thing**, который реализует возможность управления портами расширителя
		- **Bridge for Megad PCA9685 extenders** реализует поддержку  [PCA9685](https://www.ab-log.ru/smart-house/ethernet/megad-2561#conf-exp-pca), а **MegaD PCA9685 extender port selector Thing** реализует управление портом расширителя.
	1. С типами всех каналов можно ознакомиться на вкладке **Channels** веб-интерфейса **каждого из Things**
1. Текстовые файлы
	1. .things:

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
