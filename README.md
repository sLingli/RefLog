<div align="center">

<img src="https://github.com/user-attachments/assets/aa8c7baa-4e6d-43a4-9c2d-e9db0c035c2d" width="220" height="220" alt="RefLog Logo">

  <h1>RefLog</h1>
  <h3>⚽ A Professional Soccer Referee Timer | 足球裁判执法辅助系统</h3>
  
  <p>
    <a href="LICENSE">
      <img src="https://img.shields.io/badge/license-GPL%20v3-blue.svg" alt="License">
    </a>
    <img src="https://img.shields.io/badge/platform-Android-brightgreen.svg" alt="Platform">
    <img src="https://img.shields.io/badge/design-Material%20Design%203-purple.svg" alt="Design">
  </p>

  <p>
    <!-- 语言导航栏 -->
    👇 <strong>Languages / 语言</strong> 👇
  </p>
  <p>
    <a href="#-简体中文"><strong>CN 简体中文</strong></a> &nbsp;|&nbsp; <a href="#-english"><strong>EN English</strong></a>
  </p>
</div>

<br>

---

<!-- ================= CHINESE SECTION ================= -->
<h2 id="-简体中文">CN 简体中文</h2>

### 📖 项目简介
**RefLog** (Referee Logger) 源于我对足球的热爱，也源于我作为一名新晋足球裁判员的亲身体验，在 Android Studio 上独立开发的练手之作。我希望用现代化的设计语言，为足球裁判提供一个顺手的执裁工具

### 🛠️ 核心功能
1.  **专业双路计时系统 ⏱️**
    *   **主计时器 + 补时计时器**：暂停比赛时，补时计时器自动激活并记录时长
    *   **智能提醒**：当比赛达到半场时间，主计时器自动变色（绿 -> 橙），醒目提醒裁判进入伤停补时阶段
    *   <img src="https://github.com/user-attachments/assets/5068a371-5155-4c8f-8ace-7f6eac0e6271" width="220"/><img src="https://github.com/user-attachments/assets/962404e1-f616-4b3f-955f-6e71c2dd299d" width="220"/><img src="https://github.com/user-attachments/assets/aa431f0c-48b5-45f8-bd71-2170b020f22d" width="220"/>
2.  **全场景事件记录 📝**
    *   提供 **红牌、黄牌、进球、换人、取消** 等快捷选项。
    *   **逻辑闭环**：触发事件 -> 自动弹出主客队选择（UI跟随球衣颜色变化）-> 滚轮快速选号 -> 自动保存时间戳
    *   <img src="https://github.com/user-attachments/assets/0efb3825-fd9f-4532-a7aa-aea17d5894b3" width="220"/><img src="https://github.com/user-attachments/assets/3ac4be63-49d0-4fef-be29-6f831f22f1be" width="220"/><img src="https://github.com/user-attachments/assets/1c43de8e-4b09-4578-9729-4b0ea98fe4ed" width="220"/>
3.  **历史回顾与赛后总结 📊**
    *   **赛后总结**：比赛结束后，自动生成包含最终比分、所有事件时间点的总结报告
    *   **历史记录**：所有数据本地保存，随时回溯过往比赛
    *   <img src="https://github.com/user-attachments/assets/925bc9f5-f19b-47ce-99d5-d785d57c65c2" width="220"/>
4.赛前可设置比赛时间

### ✨ 核心亮点
1.  **惯性滚轮 (Physics Wheel)** 
    *   用于选择球衣颜色和队员号码，滚轮的设计方便裁判员快速选择
    *   重写了控件，加入**磁吸回弹**与**物理惯性**效果
    *   视觉上实现了**近大远小**的 3D 透视感，手感顺滑
    *   <img src="https://github.com/user-attachments/assets/8626d450-2372-4e5c-8604-8290d16569ba" width="220"/><img src="https://github.com/user-attachments/assets/d628659d-1c7b-4adf-9d24-6979b976d42c" width="220"/>

2.  **流体按钮动画** 
    *   点击“开始比赛”时，按钮不会生硬消失，而是变红并**平滑向左收缩**，同时“结束”按钮从右侧浮现。拒绝生硬的过渡
3.  **弹性侧滑删除** 
    *   历史记录列表支持**跟手左滑**。
    *   滑动过程带有**阻尼弹性**。点击删除圆圈后，条目会向左平滑飞出消失
    *   <img src="https://github.com/user-attachments/assets/0638304e-9de9-4253-99ba-0d221228fe75" width="220"/>


### 🤖 特别致谢 (AI 导师)
作为一个初学者，本项目在代码架构上得到了以下 AI 伙伴的巨大帮助。没有它们，我无法独自完成这个项目：
*   **DeepSeek**
*   **Gemini**
*   **Claude**

### 🙇‍♂️ 关于作者 & 寻找伙伴
目前 **RefLog 只有我一个人在战斗**。
作为一个小白，代码中肯定存在不规范的地方，功能也尚待完善
*   **致大佬们**：非常欢迎各位前来指点代码，提 Issue 或 PR，我会不断向大家学习
*   **致球友们**：如果你也觉得这个工具还不错，欢迎推荐给身边的裁判朋友

[回到顶部 / Back to Top](#reflog)

<br>
<br>

---

<!-- ================= ENGLISH SECTION ================= -->
<h2 id="-english">EN English</h2>

### 📖 Introduction
**RefLog** (Referee Logger) was born out of my passion for football and my personal experience as a newly qualified referee. It is an independent practice project developed in Android Studio. My goal is to use modern design language to provide an intuitive and efficient tool for soccer referees.

### 🛠️ Core Features
1.  **Professional Dual Timing System ⏱️**
    *   **Main + Stoppage Timer**: The stoppage timer activates automatically and records duration when the match is paused.
    *   **Smart Alerts**: When the match reaches the half-time duration, the Main Timer automatically changes color (Green -> Orange) to visually alert the referee that stoppage time has begun.
    *   <img src="https://github.com/user-attachments/assets/bf9a2730-f5df-4b3a-b105-d3fa069ce84e" width="220"/><img src="https://github.com/user-attachments/assets/1b274bc3-1b41-4629-81d6-92e0a9fe6de2" width="220"/><img src="https://github.com/user-attachments/assets/a5003379-0837-4fbe-8bda-22d1ef4d48a6" width="220"/>
2.  **Full-Scenario Event Logging 📝**
    *   Quick access to **Red Card, Yellow Card, Goal, Substitution, and Cancel**.
    *   **Closed-Loop Logic**: Trigger Event -> Auto-popup Team Selection (UI adapts to jersey color) -> Inertial Wheel for Number Selection -> Auto Save Timestamp.
    *   <img src="https://github.com/user-attachments/assets/605190c6-4ef9-4e83-860f-1302846e9300" width="220"/><img src="https://github.com/user-attachments/assets/77b07ce3-acf3-4867-bc3a-9ec798ab572d" width="220"/><img src="https://github.com/user-attachments/assets/cac1d333-d4c8-4895-af8a-4546a0a79559" width="220"/>
3.  **Match Summary & History 📊**
    *   **Post-Match Summary**: After the match, a report containing the final score and a timeline of all events is automatically generated.
    *   **History**: All data is stored locally, allowing you to review past matches at any time.
    *   <img src="https://github.com/user-attachments/assets/8a74133e-1245-4e11-a950-daa14ef93568" width="220"/>
4.  Match time can be set before the match

### ✨ UX Highlights
1.  **Inertial Wheel (Physics Wheel)** 
    *   Designed for selecting jersey colors and player numbers, allowing referees to make quick selections.
    *   Custom-written view with **magnetic snap** and **physics-based inertia** effects.
    *   Visually implements a **"Fisheye" (3D perspective)** effect (larger in center, smaller at edges) for a smooth tactile feel.
    *   <img src="https://github.com/user-attachments/assets/f694e408-0761-4686-9ade-784c0a794eb7" width="220"/><img src="https://github.com/user-attachments/assets/c753a343-357d-4037-a067-f9e537085a80" width="220"/>
2.  **Fluid Button Animations** 
    *   When clicking "Start Match", the button doesn't just disappear; it turns red and **shrinks smoothly to the left**, while the "End" button floats in from the right. No harsh transitions.
3.  **Elastic Swipe-to-Delete** 
    *   The history list supports a **responsive follow-hand swipe**.
    *   The sliding process features **elastic damping**. Clicking the delete circle triggers a smooth fly-out animation to the left.
    *   <img src="https://github.com/user-attachments/assets/b0a3fe24-6547-4859-998b-836641f36935" width="220"/>

### 🤖 Special Thanks (AI Mentors)
As a beginner, this project received immense help with code architecture from the following AI partners. I could not have completed this project alone without them:
*   **DeepSeek**
*   **Gemini**
*   **Claude**

### 🙇‍♂️ About Me & Call for Collaboration
Currently, **RefLog is a one-man army project.**
As a beginner, I know there are coding imperfections and features that need improvement.
*   **To Developers**: I warmly welcome any guidance, Issues, or Pull Requests. I am eager to learn from you!
*   **To Referees**: If you find this tool useful, please recommend it to your referee colleagues.

[Back to Top / 回到顶部](#reflog)
