## 😀 BE & FE Team 😀
|<img src="https://avatars.githubusercontent.com/u/37374613?v=4" width="150" height="150"/>|<img src="https://avatars.githubusercontent.com/u/183458940?v=4" width="150" height="150"/>|<img src="https://avatars.githubusercontent.com/u/163218508?v=4" width="150" height="150"/>|<img src="https://avatars.githubusercontent.com/u/133200703?v=4" width="150" height="150"/>|<img src="https://avatars.githubusercontent.com/u/109015067?v=4" width="150" height="150"/>|<img src="https://avatars.githubusercontent.com/u/62224479?v=4" width="150" height="150"/>|
|:-:|:-:|:-:|:-:|:-:|:-:|
|[BE] 최현산<br/>[@ChoiHyunSan](https://github.com/ChoiHyunSan)|[BE] 최승현<br/>[@Metronon](https://github.com/Metronon)|[BE] 노동희<br/>[@nodonghui](https://github.com/nodonghui)|[BE] 성기범<br/>[@sungkibum](https://github.com/sungkibum)|[BE] 이장호<br/>[@jho951](https://github.com/jho951)|[FE] 이성헌<br/>[@Lee-sungheon](https://github.com/Lee-sungheon)|

## 📚 BE & FE Stacks, Tools
<div align=center> 
  <img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white">
  <img src="https://img.shields.io/badge/spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white"> 
  <img src="https://img.shields.io/badge/spring boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  <br>
  <img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white">
  <img src="https://img.shields.io/badge/Redis-FF4438?style=for-the-badge&logo=redis&logoColor=white">
  <img src="https://img.shields.io/badge/elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white">
  <img src="https://img.shields.io/badge/kibana-005571?style=for-the-badge&logo=kibana&logoColor=white">
  <br>
  <br>

  <img src="https://img.shields.io/badge/node.js-339933?style=for-the-badge&logo=Node.js&logoColor=white">
  <img src="https://img.shields.io/badge/typescript-3178C6?style=for-the-badge&logo=typescript&logoColor=white">
  <img src="https://img.shields.io/badge/html5-E34F26?style=for-the-badge&logo=html5&logoColor=white"> 
  <img src="https://img.shields.io/badge/css-1572B6?style=for-the-badge&logo=css3&logoColor=white">
  <br>
  <br>

  <img src="https://img.shields.io/badge/git-F05032?style=for-the-badge&logo=git&logoColor=white">
  <img src="https://img.shields.io/badge/github-181717?style=for-the-badge&logo=github&logoColor=white">
  <img src="https://img.shields.io/badge/NGINX-009639?style=for-the-badge&logo=NGINX&logoColor=white">
  <img src="https://img.shields.io/badge/amazonaws-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white">
  <img src="https://img.shields.io/badge/githubactions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">
  <img src="https://img.shields.io/badge/docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> 
  <br>
</div>

## 🎯 프로젝트 개요
DAJAVA는 웹 사이트를 대상으로 사용자들의 행동 패턴을 대상으로 분석해 AI 솔루션, 통계치를 누적해 히트맵 데이터를 제공하고 있습니다.<br/><br/>
이를 통해 서비스 신청자는 사용자들이 대상 웹 사이트를 사용하면서 겪는 UI/UX 불편함을 확인 및 개선이 가능합니다.<br/><br/>
사용자들이 상호작용한 요소들을 분석해, 비 전문가도 쉽게 웹 사이트 개선점을 파악이 가능하도록 솔루션 제공을 목표하고 있습니다.<br/>

![image](https://github.com/user-attachments/assets/18e02c5d-fffb-4da4-8159-eb1bd20cca53)

## 🌫️ 플로우 차트
처음 프로젝트 계획시 목표했던 플로우 차트 <br/>
![5](https://github.com/user-attachments/assets/474bd1dc-dd05-4529-b21a-627fe45f8c66)

## 패키지 설명
register : DAJAVA 홈페이지에서 솔루션을 신청,완료된 솔루션을 조회하는 로직이 있는 패키지 입니다.

mouseeventsave : 솔루션 신청 웹사이트의 사용자로 부터 전송 받은 마우스 이벤트 데이터를 DB에 저장하는 로직이 있는 패키지 입니다. 컨트롤러 코드 부터 데이터 흐름이 시작됩니다.
└── infra : 임시버퍼가 java memory 또는 redis인 버전으로 나뉘어 있습니다. 현재 redis 부분은 리팩토링이 덜 된 부분이 있습니다. 

mouseeventvalidation : DB에 저장된 이벤트 데이터 중 UI 문제를 겪고 있는 마우스 이벤트를 자체 알고리즘으로 판별하는 로직이 있는 패키지 입니다. 스케줄러 코드 부터 데이터 흐름이 시작됩니다.

sessionvalidation : 이벤트를 전송하는 세션 중 악의적으로 대량의 이벤트 데이터를 전송하는 어뷰징 세션을 판별하는 로직이 있는 패키지 입니다. 스케줄러 코드 부터 데이터 흐름이 시작됩니다.

heatmap : DB에 저장된 정상 이벤트 데이터를 이용해 히트맵 데이터를 생성하는 로직과 히트맵에 사용할 솔루션 웹사이트 이미지를 관리하는 로직이 있는 패키지 입니다. 컨트롤러 코드 부터 데이터 흐름이 시작됩니다.

solution : mouseeventvalidation에서 얻은 UI 문제를 겪는 마우스 이벤트 데이터와 Gemini를 이용해 UI 개선점 텍스트를 생성하는 로직이 있는 패키지 입니다. 스케줄러 코드 부터 데이터 흐름이 시작됩니다.

