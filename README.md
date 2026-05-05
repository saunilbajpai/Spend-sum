# SpendSum

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)

SpendSum is a personal finance dashboard I built for my final year research project. The main goal was to test a "hybrid" approach to AI anomaly detection in financial data—basically trying to predict when someone is going to overspend before it actually happens.

I noticed that passing every single transaction to an LLM like Gemini is way too slow and expensive. So instead, I built a deterministic rule engine in Spring Boot that runs the math (spending velocity, budget thresholds) instantly. It acts as a filter. When a mathematical anomaly is triggered, it passes that specific context to the LLM to generate readable advice. 

This approach cut LLM token usage by over 90% and kept the core API latency under a few milliseconds.

## Tech Stack
**Backend:** Java 23, Spring Boot 3.5, MySQL, Spring Data JPA, Gemini API  
**Frontend:** React 19, Vite, Recharts, standard CSS variables for the glass UI theme

## Features
- **Hybrid Anomaly Detection:** Rule-based engine handles the math; Gemini LLM handles the advice generation.
- **Async Processing:** AI calls are decoupled from the main thread so the UI never blocks.
- **Research Metrics Tracking:** The app logs API latency, LLM confidence scores, and user feedback (helpful/not helpful) directly to the database so I could pull metrics for my paper.
- **Dev Seeder:** A built-in endpoint to flood the database with realistic transactions (weekends, rent, salary) to test the anomaly triggers.

---

## Running it locally

### 1. Database & Backend
First, make sure you have a local MySQL instance running. 
1. `cd backend`
2. Update `src/main/resources/application.properties` with your local database credentials and your Gemini API key.
3. Run it using maven:
   ```bash
   mvn spring-boot:run
   ```
The backend starts on `localhost:8080`.

### 2. Frontend
1. `cd frontend`
2. `npm install`
3. `npm run dev`

The frontend will be available at `localhost:5173`. 

### 3. Loading Test Data
If you want to test the charts and AI triggers without manually typing in 100 transactions, I wrote a seeder script. While the backend is running, just hit:
```bash
curl -X POST http://localhost:8080/api/dev/seed?reset=true
```
This will clear old data, generate a realistic month of spending, force a budget breach, and trigger the AI insights automatically.

## Screenshots

*(Add your screenshots here)*
