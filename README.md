
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
	1. Текстовые файлы

Список ID для работы с текстовыми файлами things:

|Thing     |Thing ID|Parameters | Channels|
|:--:|:--:|:--:|:--:|
| Bridge for incoming connections  |tcp  | port |
| Bridge for incoming connections  |tcp  | port |
| Bridge Megad hardware  | device  | hostname, password |
| Bridge Megad 1 wire bus port  | 1wirebus  |port, refresh|
| MegaD 1wire bus Thing  | 1wireaddress  |address|
| Bridge Megad I2C bus port   | itoc  |port, scl|
| MegaD I2C bus sensor handler  | i2cbussensor  | sensortype, refresh, rawparam|
| Bridge for Megad MCP23008/MCP23017 extenders  |  extenderport| port, refresh, int|
| MegaD MCP23008/MCP23017 extender port selector Thing  | extender  | extport|
| Bridge for Megad PCA9685 extenders  | extenderPCA9685Bridge  |port, refresh|
| MegaD PCA9685 extender port selector Thing  |  extenderPCA9685 |extport|
| MegaD Standard Thing  | standard  |port, refresh, correction, ds2413_ch|
| MegaD Group thing  | group  | groupnumber|
| MegaD I2C LCD1609 display  | lcd1609  | port|
|  MegaD I2C Sensors | i2c  | port, refresh|
|  MegaD rs485 Thing |  rs485 | type(sdm120 only), address, refresh
| MegaD thing for encoder  | encoder  | sda, scl, int|



- Создаем бридж в файле .things.
`Bridge megad:tcp:megadeviceincoming [port=8989] {}`
megad:tcp: - обязятельное поле, после двоеточия - произвольное название.
- Добавляем определение адреса меги внутрь фигурных скобок
```
Bridge megad:tcp:megadeviceincoming [port=8989]
{
	Bridge  device  mega1  "Mega 1 hardware"  [hostname="192.168.0.14", password="sec"] {
	}
}
```
device - обязательное поле, далее произвольное название

Это базовая настройка, далее сюда мы добавляем или bridge или thing

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

вариант с bridge:
```
Bridge megad:tcp:megadeviceincoming[port=8989]
{
	Bridge  device  mega1  "Mega 1 hardware"  [hostname="192.168.0.14", password="sec"] {
		Bridge  1wirebus  busN1  "Bus 1 mega1"  [port="0", refresh="30"]
			Thing 1wireaddress onewire "Датчик" [address="c6f479a20003"]
	}
}

```

#### 3) открываем .items и создаем наши переменные.

```
Number Temperature_GF_Corridor "Temperature [%.1f °C]" <temperature> (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:onewire:onewire" }
Switch MegaDBindingThing_Input "Temperature " (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:kitchenout:out" } 
Contact MegaDContact "[%s]" (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:bedroomcontact:contact" }
```

Последний параметр - режимы работы(каналы). до этого - путь, который мы создали в .things (megad:device:megadeviceincoming: - это название бриджа, bedroomcontact: - название Thing )

Donate:

[Paypal](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=P38VCVDQMSMYQ) 

[Yandex.Money](https://money.yandex.ru/to/410011024847033)

Спасибо!
