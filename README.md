# 1) Clone dự án
git clone <https://github.com/hieuoneplus/dss-system.git> 

cd dss-system

# 2) Chạy backend (cửa sổ/terminal 1)
cd backend

mvn spring-boot:run   # hoặc ./mvnw spring-boot:run

# 3) Chạy frontend (cửa sổ/terminal 2)
cd frontend

npm install --legacy-peer-deps

npm start
