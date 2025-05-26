# Weather Application

A modern, feature-rich weather application built with Java and JavaFX that provides real-time weather information for cities around the world.

## Features

- **Real-time Weather Data**: Retrieve current weather conditions including temperature, humidity, wind speed, and more  
- **City Search**: View weather information for any city worldwide  
- **Temperature Unit Toggle**: Switch between Celsius and Fahrenheit  
- **Recent Cities**: Automatically save and quickly access recently viewed cities  
- **Hot Cities Filter**: Filter cities with temperatures above a defined threshold  
- **Smart Caching**: Efficiently cache weather data to reduce API calls and boost performance  
- **Automatic Updates**: Background updates for frequently accessed cities  

## Architecture

The application follows a clean architecture with clear separation of concerns:

### Core Components

- **Model**: Plain data classes representing weather information  
- **Service**: Business logic for retrieving and processing weather data  
- **Cache**: Efficient caching system with expiration policies  
- **Queue**: Priority queue for managing city update priorities  

### UI Components

- **JavaFX UI**: Modern interface built using JavaFX and FXML  
- **Controllers**: Handle user interaction and display data  

### External Integration

- **Weather API Proxy**: Acts as a proxy for the OpenWeatherMap API to fetch weather data  

## Technologies Used

- **Java 21**: Main programming language  
- **JavaFX**: UI framework  
- **Jackson**: JSON processing library  
- **Maven**: Build and dependency management tool  
- **OpenWeatherMap API**: External source of weather data  

## How to Run

1. Make sure Java 21 or higher is installed  
2. Clone this repository  
3. Build the project using Maven: `mvn clean package`  
4. Run the application: `mvn javafx:run`  
5. Or run the JAR file directly: `java -jar target/weatherapp-1.0-SNAPSHOT.jar`

## Project Structure
src/main/java/weather/
├── core/
│ ├── cache/ # Caching mechanisms
│ ├── model/ # Data models
│ ├── queue/ # Priority queue implementation
│ └── service/ # Business logic services
├── proxy/
│ └── api/ # External API integration
├── ui/ # User interface components
│ ├── controllers/ # UI controllers
│ └── views/ # FXML view definitions
└── util/ # Utility classes

## Dependencies

- **JavaFX 21**  
- **Jackson 2.16.1**

## License

This project is open source and available under the MIT License.

