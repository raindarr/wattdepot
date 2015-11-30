# Introduction #

WattDepot deals in power data and analysis. To understand what it can do, it is necessary to understand the underlying physical concepts. This page explains power, energy, carbon, and their interrelationships.

# Power #

[Power](http://en.wikipedia.org/wiki/Power_(physics)) is defined as the rate of change for energy. As with any rate, it is expressed as a quantity of energy over a unit of time. The most common unit for power (and the one used in WattDepot) is the [watt](http://en.wikipedia.org/wiki/Watt), abbreviated as "W". One watt defined as one joule (a measure of energy) per second. You might be familiar with a 60 watt incandescent light bulb, which expresses how much power it uses when turned on.

# Energy #

[Energy](http://en.wikipedia.org/wiki/Energy_(physics)) is defined as the amount of work that can be done by a force. Most of us have an intuitive notion of energy: is makes things move, it heats things up, etc. There are many units used to measure energy: joules (a very small amount of energy), BTUs, calories. WattDepot uses the unit of a watt hour, abbreviated as "Wh", which is equal to 3600 joules. A watt hour is the amount of energy required to to provide 1 watt of power for one hour. Note that from a certain perspective it is somewhat peculiar to measure energy in units that include power (watt), since power is defined in terms of energy in the first place. This underlines how central the concept of power is in most of our dealings with electricity.

# Analogy to Cars #

Power and Energy are closely related, but frequently confused concepts. As an analogy, think about a car. We can talk about the speed of a car (in miles per hour, or kilometers per hour) and we can also talk about a distance driven in a car (miles or kilometers). The speedometer in the car measures the speed (distance over time), while the odometer measures the distance traveled. Speed is a rate, like power, while distance is like energy.

When we talk about speeds, we usually talk about instantaneous measurements of speed. A speed limit is the maximum instantaneous speed at which you are allowed to drive, i.e. the car's speedometer should never register a speed greater than the limit. However, when we talk about distance driven, it only makes sense to talk about a distance driven between two locations, or the distance driven over a particular time interval. There is no such thing as an instantaneous distance driven, because in at a precise instant in time the car is not moving.

# Power vs. Energy #

Since power is the rate of change of energy, if you know how power changes over time, you can determine how much energy was consumed or produced (the area under the power curve). Similarly, if you know how much energy was used over an interval of time, you can compute the average power over that period of time (but not the instantaneous power).

In our interactions with appliances, we usually talk about their power consumption and not their energy consumption. For example, we have 60 watt light bulbs, but we wouldn't generally talk about a 60 watt hour lightbulb (unless it consumed 60 watts and then burned out!). This is because power consumption is an intrinsic characteristic of things that use electricity, while the amount of energy is used by an electrical device is determined by how long you keep it plugged in or turned on. On the other hand, energy is very important to the utility that provides your electricity, since you are billed by how much energy you have used (typically in kilowatt hours).

The two key points to remember are: power is a **rate**, and we always talk about energy **over an interval of time**.

# Carbon #

As one of the goals of using WattDepot to collect power data is to reduce power consumption in order to combat [global warming](http://en.wikipedia.org/wiki/Global_warming), it is important to talk about carbon. The [overwhelming scientific consensus](http://ipcc-wg1.ucar.edu/wg1/Report/AR4WG1_Print_SPM.pdf) is that human activity has increased the concentration of greenhouse gasses (GHG) in the atmosphere, and this increase has led to changes in our climate, particularly an increase in global temperatures.

While there are many greenhouse gasses, carbon dioxide (CO2) one of primary concern. When fossil fuels are burned to generate electricity, CO2 is released into the atmosphere. Different fossil fuels (coal, oil, natural gas) and different power plant technologies lead to different amounts of CO2 being emitted as energy is produced. The mass of CO2 emitted per unit of energy generated is referred to as the [carbon intensity](http://en.wikipedia.org/wiki/Carbon_intensity) of the plant. We can multiply the carbon intensity of a generating facility by the amount of energy generated over a period of time to get the carbon emitted over that period of time.

While CO2 is considered the most important of the greenhouse gasses, there are other GHG that can be emitted when burning fossil fuels. Therefore carbon intensities are often stated as being masses of CO2 equivalent, meaning the warming effect of the combination of GHG emitted if it were all "converted" to CO2 for simplicity.