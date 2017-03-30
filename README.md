режимы работы: "in", "out", "dimmer", "temp", "humidity", "onewire", "adc", "at", "st", "ib", "tget", "contact", в процессе "i2c".

как запустить? 

# 1) через PaperUI

Configuration > System > Item Linking

simple mode is turned off

Save


Inbox -> MegaD Binding -> Choose Thing

Bridge Megad incoming server adapter

OK

Inbox -> MegaD Binding -> Choose Thing

MegaD Binding Thing

Bridge Selection - > Bridge Megad incoming server adapter - megad:bridge:megadeviceincoming

Configuration Parameters

OK

Configuration > Things

MegaD Binding Thing

Channels

link

# 2) через файлы

.things:

```
Bridge megad:bridge:megadeviceincoming
{
 Thing device onewire [hostname="localhost", port="3", password="sec", refresh="10"]
 Thing device kitchenout [hostname="localhost", port="1", refresh="0"]
 Thing device bedroomcontact [hostname="localhost", port="2", refresh="0"]
}
```


.items:
```
Number Temperature_GF_Corridor  "Temperature [%.1f °C]" <temperature>   (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:onewire:onewire" }
Switch MegaDBindingThing_Input  "Temperature " (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:kitchenout:out" }  
Contact MegaDContact  "[%s]" (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:bedroomcontact:contact" }
```


Принцип такой: 
### 1) Создаем бридж в файле .things.

```
Bridge megad:bridge:megadeviceincoming {}
```

megad:bridge: - обязятельное поле, после двоеточия - произвольное название.

### 2) Добавляем Thing (По сути наши порты для меги) внутрь фигурных скобок
```
Bridge megad:bridge:megadeviceincoming
{
Thing device onewire [hostname="localhost", port="3", password="sec", refresh="10"]
Thing device kitchenout [hostname="localhost", port="1", refresh="0"]
Thing device bedroomcontact [hostname="localhost", port="2", refresh="0"]
}
```
device - обязательное поле, далее произвольное название

### 3) открываем .items и создаем наши переменные.
```
Number Temperature_GF_Corridor "Temperature [%.1f °C]" <temperature> (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:onewire:onewire" }
Switch MegaDBindingThing_Input "Temperature " (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:kitchenout:out" } 
Contact MegaDContact "[%s]" (Temperature, GF_Corridor) { channel = "megad:device:megadeviceincoming:bedroomcontact:contact" }
```
Последний параметр - режимы работы(каналы). до этого - путь, который мы создали в .things (megad:device:megadeviceincoming: - это название бриджа, bedroomcontact: - название Thing )


### 4) Далее аналогично 1 версии опенхаба


По многочисленным просьбам - Donate:

[Paypal](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=P38VCVDQMSMYQ) 

[Yandex.Money](https://money.yandex.ru/to/410011024847033)

Спасибо!
