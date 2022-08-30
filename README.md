
# OpenHAB 3 MegaD binding

OpenHAB 3 MegaD binding создан для интеграции многофункционального контроллера [MegaD-2561](https://www.ab-log.ru/smart-house/ethernet/megad-2561 " ") в систему умного дома openHAB.

С помощью данного биндига реализуются приём и отправка GET-запросов на исполнительные устройства

Чтобы установить биндинг на сервер, потребуется [скачать](https://github.com/Pshatsillo/openhab2MegadBinding/releases)  файл биндинга и положить его в папку `Addons` сервера openHAB

В некоторых случаях может потребоваться выполнение команды через [консоль](https://www.openhab.org/docs/administration/console.html) команды `feature install openhab-transport-http`

В результате в веб-интерфейсе администрирования openHAB (Настройки->Things-> +)  должен появиться `MegaD Binding`



Далее есть два варианта настройки: `веб-интерфейс` или `текстовые файлы` 

## Настройка через Веб-интерфейс

1. Настраиваем `Bridge for incoming connections`. Указываем номер порта `8989`. Это сервер входящих и исходящих сообщений, который будет общаться с MegaD.
1. Настраиваем `Bridge Megad controller`. Указываем IP адрес и пароль от MegaD. Обращаю внимание, что данный бридж зависит от предыдущего и его нужно обязательно указать в настройках.
1. Далее возможны варианты настроек в зависимости от необходимиого функционала. 
`MegaD Standard Thing` содержит в себе базовый функционал MegaD-2561, такой как вход, выход, диммер, температура с внешнего датчика Onewire, подключенного `напрямую в порт`
`MegaD I2C Sensors` содержит в себе функционал для вывода информации с датчиков, подключенных по I2C `напрямую в порт`
`MegaD Group thing` управляет `созданными на устройстве группами` (GET-запрос на устройство вида `g7:1`)
`MegaD thing for encoder` реализует функционал управления устройством с помощью энкодера. В данный момент находится в тестовом виде
`MegaD I2C LCD1609 display` реализует функционал по выводу информации на двустрочный дисплей LCD1609
`MegaD rs485 Thing` реализует функционал по выводу информации по протоколу rs485. В данный момент реализован вывод со счётчика электроэнергии Eastron SDM 120 и DDS238. Так же реализован протокол обмена сообщениями с климатическими устройствами Midea
1. Кроме того, реализована возможность вывода работы с устройствами, подключенными шиной.
	- `Bridge Megad 1 wire bus port` реализует опрос 1-wire датчиков подключенных шиной, а `MegaD 1wire bus Thing` отвечает за обработку и вывод данных по выбранному датчику и зависит от `Bridge Megad 1 wire bus port`
	- `Bridge Megad I2C bus port` реализует опрос шины i2c и служит мостом для `MegaD I2C bus sensor handler`
	- `Bridge for Megad MCP23008/MCP23017 extenders` реализует поддержку расширителя [MCP23008/MCP23017](https://www.ab-log.ru/smart-house/ethernet/megad-2561#conf-exp-mcp) и является мостом для `MegaD MCP23008/MCP23017 extender port selector Thing`, который реализует возможность управления портами расширителя
	- `Bridge for Megad PCA9685 extenders` реализует поддержку  [PCA9685](https://www.ab-log.ru/smart-house/ethernet/megad-2561#conf-exp-pca), а `MegaD PCA9685 extender port selector Thing` реализует управление портом расширителя.
	1. С типами всех каналов можно ознакомиться на вкладке `Channels` веб-интерфейса `каждого из Things`

## Настройка через текстовые файлы - Things

Список ID для работы с текстовыми файлами `.things`:

|Thing     |Thing ID|Parameters | Channels|
|:--:|:--:|:--:|:--:|
| Bridge for incoming connections  |tcp  | port |
| Bridge Megad controller  | device  | hostname, password |
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
| MegaD I2C Sensors | i2c  | port, refresh|
| MegaD rs485 Thing | rs485 | type(sdm120 only), address, refresh
| MegaD thing for encoder  | encoder  | sda, scl, int|



- Создаем `Bridge megad:tcp:incoming` с указанием обязательных параметров и порта в файле `.things` - это сервер входящих и исходящих сообщений, который будет общаться с MegaD:

```
Bridge megad:tcp:incoming [port=8989]{
	...
}
```

`megad:tcp:` - обязятельное поле, `incoming` - произвольное название

`port` - обязательное поле, номер порта входящих сообщений от контроллера MegaD

- Внутри секции `Bridge megad:tcp:incoming` cоздаем `Bridge device` с указанием IP адреса и пароля контроллера MegaD. Эта секция отвечает за конкретный контроллер MegaD:

```
Bridge megad:tcp:incoming [port=8989]{ 
	Bridge device mega1 "MegaD1 controller" [hostname="192.168.0.14", password="sec"]{ 
		...
	}
}
```

`device` - обязательное поле, далее произвольное название

`hostname` - обязательное поле, IP адрес контроллера MegaD

`password` - обязательное поле, пароль контроллера MegaD
 

## Настройка через файлы - Базовый пример

- Внутри секции `Bridge device` cоздаем порты контроллера MegaD, каждый порт или датчик это новый `Thing standard`:

```
Bridge megad:tcp:incoming [port=8989]{
    Bridge device mega1 "MegaD1 controller" [hostname="192.168.0.14", password="sec"]{
        Thing standard roombut1        "Выключатель1"  [port="1", refresh="0"]
        Thing standard bedroomcontact1 "Окно1"         [port="2", refresh="0"]
        Thing standard onewire1        "Датчик1"       [port="3", refresh="10"]
        Thing standard kitchenout1     "Люстра кухня"  [port="10", refresh="0"]
    }
}
```

`standard` - обязательное поле, далее произвольное название

`port` - обязательное поле, номер порта контроллера MegaD

`refresh` - обязательное поле, интервал опроса порта контроллера MegaD в секундах, `"0"` - обработка только входящих сообщений (выключатель, геркон)

- Пример настроек `.items` файла:

```
Switch  roomBut1        "Выключатель1 [%s]" <lamp>        {channel="megad:standard:incoming:mega1:roombut1:in"}
Contact bedroomContact1 "Окно1 [%s]"        <window>      {channel="megad:standard:incoming:mega1:bedroomcontact1:contact"} 
Number  tempSens1       "Датчик1 [%.1f °C]" <temperature> {channel="megad:standard:incoming:mega1:onewire1:onewire"}
Switch  kitchenout1     "Люстра кухня"      <lamp>        {channel="megad:standard:incoming:mega1:kitchenout1:out" } 
```

`channel` - полный путь канала лучше всего посмотреть в веб-интерфейсе - Settings > Things > ввести в строке поиска `roomBut1` - имя созданного выше `Thing`

## Настройка через файлы - Опрос 1-wire датчиков подключенных шиной

- Внутри секции `Bridge device` cоздаем `Bridge 1wirebus` с указанием обязательных параметров `port` и `refresh`
- Добавляем 1wire датчики, каждый датчик это новый `Thing 1wireaddress`:

```
Bridge megad:tcp:incoming [port=8989]
{
    Bridge device mega1 "MegaD1 controller" [hostname="192.168.0.14", password="sec"] {
        Bridge 1wirebus owbus1 "Bus1 mega1" [port="0", refresh="30"]{
           Thing 1wireaddress temp1 "Датчик1" [address="c6f479a20003"]
           Thing 1wireaddress temp2 "Датчик2" [address="e6f479a20055"]
           ...
        }
    }
}
```

`1wireaddress` - обязательное поле, далее произвольное название

`port` - обязательное поле, номер порта контроллера MegaD

`refresh` - обязательное поле, интервал опроса порта контроллера MegaD в секундах

`address` - обязательное поле, адрес датчика на шине 1wire 

- Пример настроек `.items` файла:

```
Number tempSens1 "Датчик1 [%.1f °C]" <temperature> {channel="megad:1wireaddress:incoming:mega1:owbus1:temp1"}
Number tempSens2 "Датчик2 [%.1f °C]" <temperature> {channel="megad:1wireaddress:incoming:mega1:owbus1:temp2"}
```

`channel` - полный путь канала лучше всего посмотреть в веб-интерфейсе - Settings > Things > ввести в строке поиска `temp1` - имя созданного выше `Thing`

## Настройка через файлы - Опрос I2C датчиков

- Внутри секции `Bridge device` cоздаем `Bridge itoc` с указанием обязательных параметров `port` и `scl`
- Добавляем I2C датчики, каждый датчик это новый `Thing i2cbussensor`:

```
Bridge megad:tcp:incoming [port=8989]
{
    Bridge device mega1 "MegaD1 controller" [hostname="192.168.0.14", password="sec"] {
        Bridge itoc i2cbus1 "Bus1 mega1" [port="30", scl="31"]{
            Thing i2cbussensor light1 "Датчик освещенности" [sensortype="max44009", refresh="30"]
            Thing i2cbussensor sens1  "Датчик темп и влаж"  [sensortype="sht31", refresh="30"]
            ...
        }
    }
}
```

`i2cbussensor` - обязательное поле, далее произвольное название

`port` - обязательное поле, номер порта контроллера MegaD, линия `SDA` I2C датчика

`scl` - обязательное поле, номер порта контроллера MegaD, линия `SCL` I2C датчика

`sensortype` - обязательное поле, тип подключенного датчика, [см. документацию](https://www.ab-log.ru/smart-house/ethernet/megad-2561#conf-i2c)

`refresh` - обязательное поле, интервал опроса порта контроллера MegaD в секундах

- Пример настроек `.items` файла:

```
Number lightSens1 "Освещенность1 [%.2f lux]" <sun>         {channel="megad:i2cbussensor:incoming:mega1:i2cbus1:light1:par0"}
Number tempSens1  "Температура1 [%.1f°]"     <temperature> {channel="megad:i2cbussensor:incoming:mega1:i2cbus1:sens1:par1"}
Number humSens1   "Влажность1 [%.0f%%]"      <humidity>    {channel="megad:i2cbussensor:incoming:mega1:i2cbus1:sens1:par0"}
```

`channel` - полный путь канала лучше всего посмотреть в веб-интерфейсе - Settings > Things > ввести в строке поиска `light1` - имя созданного выше `Thing`

## Настройка через файлы - Шина RS485

- Внутри секции `Bridge device` добавляем RS485 устройства, каждое устройство это новый `Thing rs485`:

```
Bridge megad:tcp:incoming [port=8989]{
    Bridge device mega1 "MegaD1 controller" [hostname="192.168.0.14", password="sec"] {
        Thing rs485 id1 "Устройство id1" [type="dds238", refresh="3",address="01"]
        Thing rs485 id2 "Устройство id2" [type="dds238", refresh="3",address="02"]
        ...
    }
}
```

`type` - обязательное поле, тип устройства

`refresh` - обязательное поле, интервал опроса шины RS485 в секундах

`address` - обязательное поле, адрес устройства на шине RS485

- Пример настроек `.items` файла для счетчика [DDS238-1 ZN](https://www.ab-log.ru/smart-house/ethernet/megad-rs485):

```
Number powerCounter    "Счётчик [%.2f kW]"              <energy> {channel="megad:rs485:mega1:id1:totalactnrg"}
Number powerVoltage    "Напряжение [%.0f V]"            <energy> {channel="megad:rs485:mega1:id1:voltage"}
Number powerCurrent    "Ток [%.2f А]"                   <energy> {channel="megad:rs485:mega1:id1:current"}
Number powerLoad       "Нагрузка [%.0f W]"              <energy> {channel="megad:rs485:mega1:id1:activepower"}
Number powerReactLoad  "Реактивная Нагрузка [%.0f VAr]" <energy> {channel="megad:rs485:mega1:id1:apparentpower"}
Number powerPF         "Power Factor [%.1f PF]"         <energy> {channel="megad:rs485:mega1:id1:powerfactor"}
Number powerFreq       "Частота [%.2f Hz]"              <energy> {channel="megad:rs485:mega1:id1:frequency"}
```

`channel` - полный путь канала лучше всего посмотреть в веб-интерфейсе - Settings > Things > ввести в строке поиска `id1` - имя созданного выше `Thing`

## Настройка через файлы - Общий пример

```
Bridge megad:tcp:incoming [port=8989]{
    Bridge device mega1 "MegaD1 controller" [hostname="192.168.0.14", password="sec"] {
        Thing standard roombut1        "Выключатель1"  [port="1", refresh="0"]
        Thing standard bedroomcontact1 "Окно1"         [port="2", refresh="0"]
        Thing standard onewire1        "Датчик1"       [port="3", refresh="10"]
        Thing standard kitchenout1     "Люстра кухня"  [port="10", refresh="0"]

        Bridge 1wirebus owbus1 "Bus1 mega1" [port="0", refresh="30"]{
           Thing 1wireaddress temp1 "Датчик1" [address="c6f479a20003"]
           Thing 1wireaddress temp2 "Датчик2" [address="e6f479a20055"]
        }

        Bridge itoc i2cbus1 "Bus1 mega1" [port="30", scl="31"]{
            Thing i2cbussensor light1 "Датчик освещенности" [sensortype="max44009", refresh="30"]
            Thing i2cbussensor sens1  "Датчик темп и влаж"  [sensortype="sht31", refresh="30"]
        }

        Thing rs485 id1 "Устройство id1" [type="dds238", refresh="3",address="01"]
        Thing rs485 id2 "Устройство id2" [type="dds238", refresh="3",address="02"]
    }
}
```

Donate: [Ю.Money](https://yoomoney.ru/to/410011024847033)

Спасибо!
