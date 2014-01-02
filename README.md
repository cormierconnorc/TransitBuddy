##About the project

[Available on Google Play](https://play.google.com/store/apps/details?id=com.connorsapps.pathfinder)

Compare the speed of walking, biking, or driving with the speed of taking the bus in any TransLoc-supported transit system!

Programmed at UVa by Connor Cormier.

Idea and UI design created with help from:
Kyle Sullivan
Meghan Pinezich
Steven Woodrum 
Anna Ewing

This project uses the TransLoc API to gather information about transit routes and arrival times. This information is used in BusPathfinder.java to calculate the best route between two stops. The Google Places API is used to find the location associated with a user-entered destination, and the Google Directions API is used to find the route to compare with the calculated transit route.

###Running the project

Since this program utilizes the Google Maps Android API v2, it cannot be run on an emulator, and the API key in the AndroidManifest.xml file will need to be updated if you intend to run it.