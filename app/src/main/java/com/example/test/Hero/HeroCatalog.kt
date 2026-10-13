package com.example.test.Hero

import com.example.test.R

/** Verified references and explicitly labeled modern applications, bundled for offline browsing. */
object HeroCatalog {
    const val ALL_FIELDS = "전체"

    val fields: List<String> = listOf(ALL_FIELDS, "정치", "과학", "예술", "문학", "군사", "철학", "의료", "사회", "교육", "환경", "탐험", "수학", "학습")

    val original: List<Hero> = listOf(
        Hero(
            id = "sejong",
            name = "세종대왕",
            field = "정치 · 과학 · 문화",
            info = "조선 · 1397–1450",
            imageResId = R.drawable.portrait_sejong,
            bio = "백성의 생활과 배움을 살핀 조선의 왕입니다. 실록에 기록된 독서와 교정 작업을 바탕으로, 읽고 확인하며 쉬는 습관을 실천합니다.",
            portraitCredit = "광화문 세종 동상 사진. 생전 초상화가 아닙니다. Republic of  Korea · CC BY-SA 2.0\nhttps://commons.wikimedia.org/wiki/File:Statue_of_King_Sejong.jpg\n비율 유지·축소·JPEG 변환. https://creativecommons.org/licenses/by-sa/2.0",
            routines = listOf(
                HistoricalRoutine(
                    id = "sejong_night_reading",
                    title = "등불 아래 한 쪽 읽기",
                    suggestedTime = "20:30",
                    historicalFact = "세종 5년 8월 4일 실록은 임금의 밤 독서에 쓸, 연기가 적고 밝은 기름을 마련하도록 했다고 기록합니다. 특정 시작 시각이나 매일 같은 분량은 기록되어 있지 않습니다.",
                    modernPractice = "눈이 편한 조명에서 책 한 쪽을 읽고 새로 알게 된 내용을 한 줄 적습니다. 밤샘 독서를 따라 할 필요는 없습니다.",
                    source = HistoricalSource(
                        title = "세종실록 21권",
                        url = "https://sillok.history.go.kr/id/kda_10508004_002",
                        locator = "세종 5년 8월 4일 2번째 기사 · 칠목 열매와 밤 독서"
                    ),
                    historicalTime = "밤 · 정확한 시각 미기록"
                ),
                HistoricalRoutine(
                    id = "sejong_review_and_rest",
                    title = "읽은 내용 교정하고 쉬기",
                    suggestedTime = "21:00",
                    historicalFact = "세종 16년 12월 11일 실록에는 매일 저녁 올라오는 자치통감훈의를 세종이 직접 교정했다고 나옵니다. 눈을 염려하는 신하의 말을 듣고 조금 쉬겠다고 답한 내용도 있습니다.",
                    modernPractice = "오늘 쓴 문장이나 공부한 노트에서 한 가지 오류를 고친 뒤 화면과 책에서 눈을 떼고 휴식합니다.",
                    source = HistoricalSource(
                        title = "세종실록 66권",
                        url = "https://sillok.history.go.kr/id/kda_11612011_004",
                        locator = "세종 16년 12월 11일 4번째 기사 · 자치통감훈의 교정"
                    ),
                    historicalTime = "저녁·밤 · 정확한 시각 미기록"
                ),
            )
        ),
        Hero(
            id = "yi_sun_sin",
            name = "이순신",
            field = "군사 · 정치",
            info = "조선 · 1545–1598",
            imageResId = R.drawable.portrait_yi_sun_sin,
            bio = "수군을 준비하고 전쟁 중에도 기록을 남긴 지휘관입니다. 군사 활동 자체가 아닌, 준비 상태를 확인하고 경험을 남기는 방법을 일상에 적용합니다.",
            portraitCredit = "전쟁기념관 이순신 흉상 사진. 생전 초상화가 아닙니다. Unknown, War Memorial of Korea · KOGL Type 1\nhttps://commons.wikimedia.org/wiki/File:Bust_of_Yi_Sun-sin_01.jpg\n비율 유지·축소·JPEG 변환. http://www.kogl.or.kr/info/licenseType1.do",
            routines = listOf(
                HistoricalRoutine(
                    id = "yi_sun_sin_readiness",
                    title = "시작 전 준비물 점검",
                    suggestedTime = "08:00",
                    historicalFact = "국사편찬위원회 신편 한국사는 이순신이 전라좌수사 부임 후 군기와 시설을 점검하고, 1592년 2월 하순 관내 여러 진의 준비 상태를 확인했다고 설명합니다. 난중일기의 임진년 정월~3월 기사를 근거로 제시합니다.",
                    modernPractice = "출근·공부·운동 전에 필요한 도구 세 가지와 오늘의 위험 요소 한 가지를 확인합니다.",
                    source = HistoricalSource(
                        title = "신편 한국사 29권 · 임란 전의 해방체제와 전라좌수군",
                        url = "https://db.history.go.kr/diachronic/level.do?levelId=nh_029_0020_0020_0030_0010",
                        locator = "이순신 부임 후 군기·군사시설 점검 문단 및 주32"
                    ),
                    historicalTime = null
                ),
                HistoricalRoutine(
                    id = "yi_sun_sin_daily_record",
                    title = "하루의 상황을 기록",
                    suggestedTime = "21:30",
                    historicalFact = "이순신은 전란 중 난중일기를 남겼습니다. 국사편찬위원회 신편 한국사는 이 기록을 생활사를 구체적으로 보여주는 전쟁 일기로 설명합니다. 매일 밤 9시에 썼다는 시간표는 확인하지 않았습니다.",
                    modernPractice = "오늘 한 일, 달라진 상황, 내일 확인할 일을 각각 한 줄씩 기록합니다.",
                    source = HistoricalSource(
                        title = "신편 한국사 31권 · 일기의 작성",
                        url = "https://db.history.go.kr/diachronic/level.do?levelId=nh_031_0050_0060_0030_0050",
                        locator = "Ⅳ 학문과 종교 → 6 역사학 → 3 당대사의 편찬 → (5) 일기의 작성"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "einstein",
            name = "알베르트 아인슈타인",
            field = "과학 · 물리학",
            info = "독일·스위스·미국 · 1879–1955",
            imageResId = R.drawable.portrait_einstein,
            bio = "물리학 연구와 함께 음악을 즐겼습니다. 동시대인의 대화 기록에 남아 있는 바이올린 연주 경험을, 집중을 전환하는 시간으로 가져옵니다.",
            portraitCredit = "Oren Jack Turner · Public domain\nhttps://commons.wikimedia.org/wiki/File:Albert_Einstein_Head_cleaned.jpg\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "einstein_music",
                    title = "집중을 바꾸는 음악 시간",
                    suggestedTime = "18:30",
                    historicalFact = "아인슈타인과 대화한 알렉산더 모슈코프스키는 그가 바이올린을 연주했으며 브람스 소나타 연주를 직접 들었다고 기록합니다. 이는 동시대인의 관찰이며 매일의 연주 시각을 증명하는 기록은 아닙니다.",
                    modernPractice = "악기를 10분 연습하거나 음악 한 곡을 다른 작업 없이 듣습니다. 곡을 들으며 떠오른 느낌을 한 줄 남깁니다.",
                    source = HistoricalSource(
                        title = "Einstein, the Searcher · Alexander Moszkowski",
                        url = "https://www.gutenberg.org/ebooks/70678",
                        locator = "Chapter XI · Einstein’s Life and Personality · Brahms Sonata와 바이올린 연주 문단"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "newton",
            name = "아이작 뉴턴",
            field = "과학 · 수학",
            info = "영국 · 1643–1727",
            imageResId = R.drawable.portrait_newton,
            bio = "빛과 운동을 관찰과 실험으로 탐구한 과학자입니다. Opticks에 드러난 실험과 불확실성을 구분하는 방법을 짧은 학습 기록으로 실천합니다.",
            portraitCredit = "Godfrey Kneller · Public domain\nhttps://commons.wikimedia.org/wiki/File:Portrait_of_Sir_Isaac_Newton,_1689_(brightened).jpg\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "newton_test_one_question",
                    title = "작은 질문 하나 검증",
                    suggestedTime = "16:30",
                    historicalFact = "뉴턴은 Opticks 제1권 첫머리에서 빛의 성질을 추측만으로 설명하는 대신 이성과 실험으로 제시하고 입증하겠다고 밝힙니다. 정해진 매일 실험 시간표가 아니라 실제 저술에 나타난 연구 방식입니다.",
                    modernPractice = "공부하거나 일하며 생긴 질문 하나를 고르고, 안전한 관찰·계산·자료 대조로 답을 확인합니다. 예상과 결과를 따로 적습니다.",
                    source = HistoricalSource(
                        title = "Opticks · Isaac Newton",
                        url = "https://www.gutenberg.org/ebooks/33504",
                        locator = "제1권 제1부 첫 문단 · Reason and Experiments"
                    ),
                    historicalTime = null
                ),
                HistoricalRoutine(
                    id = "newton_uncertainty",
                    title = "모르는 것을 질문으로 남기기",
                    suggestedTime = "20:00",
                    historicalFact = "뉴턴은 Opticks 제2판 광고문에서 중력의 원인에 충분한 실험이 없으므로 이를 질문으로 제안한다고 설명합니다. 확인한 결과와 더 탐구할 문제를 구분했습니다.",
                    modernPractice = "오늘 아직 확인하지 못한 주장 하나에 물음표를 붙이고, 다음에 확인할 자료나 실험 한 가지를 적습니다.",
                    source = HistoricalSource(
                        title = "Opticks · Isaac Newton",
                        url = "https://www.gutenberg.org/ebooks/33504",
                        locator = "Advertisement to the Second Edition · 1717년 7월 16일 · 중력의 원인에 관한 Question"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "da_vinci",
            name = "레오나르도 다 빈치",
            field = "예술 · 과학",
            info = "이탈리아 · 1452–1519",
            imageResId = R.drawable.portrait_da_vinci,
            bio = "회화와 자연 탐구를 노트에 함께 남겼습니다. 그의 노트에 쓰인 관찰과 발상 훈련을 실천하되, 권장한 방법과 실제 하루 일과를 구분합니다.",
            portraitCredit = "프란체스코 멜치에게 귀속되는 레오나르도 초상. Attributed to Francesco Melzi · Public domain\nhttps://commons.wikimedia.org/wiki/File:Francesco_Melzi_-_Portrait_of_Leonardo_(colour_correction).png\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "da_vinci_observation_sketch",
                    title = "눈에 보이는 것을 짧게 스케치",
                    suggestedTime = "12:30",
                    historicalFact = "레오나르도의 노트 571번은 사람의 동작을 관찰하고 작은 책에 간단한 선으로 기록하라고 권합니다. 이는 노트에 남긴 학습 지침이며 그가 매일 점심에 했다는 뜻은 아닙니다.",
                    modernPractice = "주변 사물이나 동작 하나를 3분간 관찰하고 간단한 선과 단어로 기록합니다. 사람을 그릴 때에는 사생활을 존중합니다.",
                    source = HistoricalSource(
                        title = "The Notebooks of Leonardo da Vinci · Jean Paul Richter 편",
                        url = "https://www.gutenberg.org/ebooks/5000",
                        locator = "노트 571 · Of the way to learn to compose figures"
                    ),
                    historicalTime = null
                ),
                HistoricalRoutine(
                    id = "da_vinci_pattern_imagination",
                    title = "무늬에서 새 형태 찾기",
                    suggestedTime = "17:30",
                    historicalFact = "노트 508번에서 레오나르도는 얼룩진 벽이나 여러 돌의 무늬에서 풍경과 형상을 발견하는 발상 방법을 설명합니다. 기록된 창작 훈련을 현대적으로 적용한 루틴입니다.",
                    modernPractice = "구름·나뭇결·바닥의 무늬에서 떠오르는 형태를 세 가지 적거나 그립니다.",
                    source = HistoricalSource(
                        title = "The Notebooks of Leonardo da Vinci · Jean Paul Richter 편",
                        url = "https://www.gutenberg.org/ebooks/5000",
                        locator = "노트 508 · A way of developing and arousing the mind to various inventions"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "shakespeare",
            name = "윌리엄 셰익스피어",
            field = "문학 · 연극",
            info = "영국 · 1564–1616",
            imageResId = R.drawable.portrait_shakespeare,
            bio = "희곡과 소네트로 알려진 작가입니다. 신뢰할 만한 개인 생활 시간표를 확인하지 못했으므로, 여기의 실천은 작품에 실제로 나타난 표현 방법에 바탕을 둡니다.",
            portraitCredit = "존 테일러에게 귀속되는 찬도스 초상. Attributed to John Taylor · Public domain\nhttps://commons.wikimedia.org/wiki/File:William_Shakespeare_by_John_Taylor,_edited.jpg\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "shakespeare_metaphor",
                    title = "한 장면을 다른 말로 표현",
                    suggestedTime = "19:30",
                    historicalFact = "소네트 18번은 사랑하는 대상을 여름날과 비교한 뒤 계절과 시간의 한계를 대비합니다. 이는 작품에서 확인되는 비유 방식입니다. 작가가 매일 이 시간에 글을 썼다는 일상 기록은 아닙니다.",
                    modernPractice = "오늘 경험한 장면을 먼저 사실대로 한 문장 쓰고, 다른 대상에 빗댄 문장으로 다시 씁니다.",
                    source = HistoricalSource(
                        title = "Shakespeare's Sonnets · William Shakespeare",
                        url = "https://www.gutenberg.org/ebooks/1041",
                        locator = "Sonnet 18 · Shall I compare thee to a summer’s day?"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "franklin",
            name = "벤저민 프랭클린",
            field = "정치 · 과학 · 문학",
            info = "미국 · 1706–1790",
            imageResId = R.drawable.portrait_franklin,
            bio = "인쇄업자이자 사상가, 정치가였습니다. 자서전의 하루 계획표와 덕목 점검표를 바탕으로 계획과 회고를 연결합니다. 계획을 완벽히 지키기 어려웠다는 고백도 함께 읽습니다.",
            portraitCredit = "Joseph-Siffred Duplessis · Public domain\nhttps://commons.wikimedia.org/wiki/File:Joseph_Siffrein_Duplessis_-_Benjamin_Franklin_-_Google_Art_Project.jpg\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "franklin_morning_intention",
                    title = "오늘의 좋은 일 정하기",
                    suggestedTime = "07:00",
                    historicalFact = "프랭클린의 자서전 하루 계획표는 오전 5~7시 구간에 기상·씻기·하루 계획·공부·아침 식사를 두고, 오늘 어떤 좋은 일을 할지 묻습니다. 자서전은 이 계획을 정확히 지키기 어려웠다고도 기록합니다.",
                    modernPractice = "오늘 가장 중요한 일 한 가지와 누군가에게 도움이 될 작은 행동 한 가지를 적습니다.",
                    source = HistoricalSource(
                        title = "The Autobiography of Benjamin Franklin",
                        url = "https://www.gutenberg.org/ebooks/148",
                        locator = "제2부 · scheme of employment · THE MORNING"
                    ),
                    historicalTime = "계획표의 오전 5~7시 구간"
                ),
                HistoricalRoutine(
                    id = "franklin_evening_review",
                    title = "오늘을 돌아보는 세 줄",
                    suggestedTime = "21:00",
                    historicalFact = "같은 계획표의 저녁 6~9시 구간에는 물건 정리·저녁 식사·음악 또는 대화·하루 점검이 들어갑니다. 저녁에는 오늘 무슨 좋은 일을 했는지 묻습니다.",
                    modernPractice = "잘한 일, 아쉬운 일, 내일 바꿀 일을 한 줄씩 적고 사용한 물건을 제자리에 둡니다.",
                    source = HistoricalSource(
                        title = "The Autobiography of Benjamin Franklin",
                        url = "https://www.gutenberg.org/ebooks/148",
                        locator = "제2부 · 하루 계획표 · EVENING"
                    ),
                    historicalTime = "계획표의 오후 6~9시 구간"
                ),
                HistoricalRoutine(
                    id = "franklin_one_virtue",
                    title = "한 가지 덕목만 점검",
                    suggestedTime = "21:10",
                    historicalFact = "프랭클린은 13가지 덕목에 대한 점검표를 만들고 한 번에 한 덕목에 주의를 집중하는 방법을 자서전에 설명합니다. 이 실천은 중단과 재개를 거쳤으며 완벽한 성취담이 아닙니다.",
                    modernPractice = "이번 주에 연습할 덕목 하나를 고르고, 오늘 그것과 관련된 행동 하나를 적습니다. 실패한 날도 기록으로 남깁니다.",
                    source = HistoricalSource(
                        title = "The Autobiography of Benjamin Franklin",
                        url = "https://www.gutenberg.org/ebooks/148",
                        locator = "제2부 · 13 virtues · 한 덕목에 일주일 집중하는 점검표"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "darwin",
            name = "찰스 다윈",
            field = "과학 · 생물학",
            info = "영국 · 1809–1882",
            imageResId = R.drawable.portrait_darwin,
            bio = "자연을 세밀하게 관찰하고 자신의 생각에 불리한 근거도 기록했습니다. 자서전에 남긴 산책과 메모 습관을, 관찰과 열린 사고의 루틴으로 적용합니다.",
            portraitCredit = "Charles_Darwin_seated.jpg: Henry Maull (1829–1914) and John Fox (1832–1907) (Maull & Fox) [3]\nderivative work: Beao · Public domain\nhttps://commons.wikimedia.org/wiki/File:Charles_Darwin_seated_crop.jpg\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "darwin_walk_and_notice",
                    title = "산책에서 차이 하나 발견",
                    suggestedTime = "12:00",
                    historicalFact = "다윈은 케임브리지 시절 헨슬로와 대부분의 날에 긴 산책을 했고, 헨슬로와 걷는 사람으로 불렸다고 자서전에 적었습니다. 점심 산책이라는 시각은 앱의 제안입니다.",
                    modernPractice = "10분간 걸으며 어제와 달라진 식물·날씨·거리의 모습 하나를 기록합니다.",
                    source = HistoricalSource(
                        title = "The Autobiography of Charles Darwin",
                        url = "https://www.gutenberg.org/ebooks/2010",
                        locator = "케임브리지 시절 · the man who walks with Henslow"
                    ),
                    historicalTime = null
                ),
                HistoricalRoutine(
                    id = "darwin_counterevidence",
                    title = "반대 근거를 바로 메모",
                    suggestedTime = "15:00",
                    historicalFact = "다윈은 자신의 일반적 결론과 반대되는 새 사실·관찰·생각을 즉시 빠짐없이 메모하는 원칙을 여러 해 지켰다고 썼습니다. 유리한 근거보다 불리한 근거를 더 쉽게 잊는다고 설명했습니다.",
                    modernPractice = "내 생각과 다른 근거 하나를 찾아 바로 적고, 기존 결론을 바꿀 필요가 있는지 검토합니다.",
                    source = HistoricalSource(
                        title = "The Autobiography of Charles Darwin",
                        url = "https://www.gutenberg.org/ebooks/2010",
                        locator = "My Several Publications · golden rule · opposed to my general results"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "curie",
            name = "마리 퀴리",
            field = "과학 · 화학",
            info = "폴란드·프랑스 · 1867–1934",
            imageResId = R.drawable.portrait_curie,
            bio = "정밀한 측정과 반복 확인으로 방사성 물질을 연구했습니다. 위험한 역사적 실험을 재현하지 않고, 기록과 검증의 태도를 일상에 가져옵니다.",
            portraitCredit = "Henri Manuel · Public domain\nhttps://commons.wikimedia.org/wiki/File:Marie_Curie_c._1920s.jpg\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "curie_measure_before_guess",
                    title = "추측 전에 한 가지 측정",
                    suggestedTime = "09:30",
                    historicalFact = "퀴리는 자전적 기록에서 새로운 현상을 연구할 때 먼저 정확히 측정해야 한다고 생각했고, 더 정밀한 장치를 선택했다고 설명합니다. 또 예상보다 강한 광물의 방사능을 주의 깊게 재확인했다고 적었습니다.",
                    modernPractice = "학습 시간·걸음 수·작업 건수처럼 안전하게 측정할 수 있는 값 하나를 같은 기준으로 기록합니다. 느낌과 수치를 구분합니다.",
                    source = HistoricalSource(
                        title = "Pierre Curie · Marie Curie",
                        url = "https://www.gutenberg.org/ebooks/69617",
                        locator = "Autobiographical Notes · II · first thing to do was to measure the phenomenon with precision"
                    ),
                    historicalTime = null
                ),
                HistoricalRoutine(
                    id = "curie_shared_outdoor_time",
                    title = "바깥에서 함께 쉬기",
                    suggestedTime = "18:00",
                    historicalFact = "퀴리는 자전적 기록에서 자신과 피에르가 가능할 때 시골에서 지냈고, 주요 이동 수단이 자전거였다고 회상합니다. 이는 여가에 대한 기록이며 매일 같은 시간의 일정은 아닙니다.",
                    modernPractice = "가능하면 가족·친구와 10분 걷거나 안전한 곳에서 가볍게 자전거를 탑니다. 오늘 관찰한 것을 하나 나눕니다.",
                    source = HistoricalSource(
                        title = "Pierre Curie · Marie Curie",
                        url = "https://www.gutenberg.org/ebooks/69617",
                        locator = "Autobiographical Notes · II · summer vacations · bicycles"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "nightingale",
            name = "플로렌스 나이팅게일",
            field = "의료 · 사회",
            info = "영국 · 1820–1910",
            imageResId = R.drawable.portrait_nightingale,
            bio = "간호 환경과 정확한 관찰의 중요성을 글로 남겼습니다. 간호 지침을 쓴 기록과 개인 생활 습관은 구분하고, 오늘의 건강한 작업 환경 점검으로 적용합니다.",
            portraitCredit = "Henry Hering (1814-1893) · Public domain\nhttps://commons.wikimedia.org/wiki/File:Florence_Nightingale_(H_Hering_NPG_x82368).jpg\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "nightingale_fresh_environment",
                    title = "내 공간의 공기와 빛 확인",
                    suggestedTime = "08:30",
                    historicalFact = "나이팅게일은 Notes on Nursing의 환기와 보온 장에서 차갑게 만들지 않으면서 공기를 깨끗하게 유지하는 것을 첫 원칙으로 제시합니다. 이는 간호 지침이며 본인의 아침 시간표가 아닙니다.",
                    modernPractice = "실내 공기·온도·빛을 확인하고 여건에 맞게 환기합니다. 추위나 외부 공기 상태가 좋지 않으면 다른 방법으로 쾌적하게 정돈합니다.",
                    source = HistoricalSource(
                        title = "Notes on Nursing · Florence Nightingale",
                        url = "https://www.gutenberg.org/ebooks/12439",
                        locator = "I · Ventilation and Warming · The very first canon"
                    ),
                    historicalTime = null
                ),
                HistoricalRoutine(
                    id = "nightingale_observe_not_guess",
                    title = "느낌 대신 관찰 세 가지",
                    suggestedTime = "20:30",
                    historicalFact = "Observation of the Sick 장은 무엇을 어떻게 관찰하고 어떤 변화가 중요한지 배우는 일을 강조합니다. 아래의 루틴은 이 관찰 원칙을 적용한 것이며 의료 진단을 대신하지 않습니다.",
                    modernPractice = "오늘의 수면·활동·기분에서 관찰 가능한 사실을 각각 적습니다. 지속되는 건강 문제는 기록만으로 판단하지 말고 전문가와 상의합니다.",
                    source = HistoricalSource(
                        title = "Notes on Nursing · Florence Nightingale",
                        url = "https://www.gutenberg.org/ebooks/12439",
                        locator = "XIII · Observation of the Sick · what to observe—how to observe"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "marcus_aurelius",
            name = "마르쿠스 아우렐리우스",
            field = "정치 · 철학",
            info = "로마 · 121–180",
            imageResId = R.drawable.portrait_marcus_aurelius,
            bio = "스스로를 위한 성찰을 명상록에 남긴 로마 황제입니다. 자기 훈계를 쓴 사실과 실제 하루 행동을 구분하며, 오늘 할 일을 차분히 정리합니다.",
            portraitCredit = "마르쿠스 아우렐리우스의 고대 대리석 흉상 사진. Daniel Martin · CC BY-SA 4.0\nhttps://commons.wikimedia.org/wiki/File:MSR-ra-61-b-1-DM.jpg\n비율 유지·축소·JPEG 변환. https://creativecommons.org/licenses/by-sa/4.0",
            routines = listOf(
                HistoricalRoutine(
                    id = "marcus_aurelius_morning_purpose",
                    title = "일어나는 이유 한 문장",
                    suggestedTime = "07:30",
                    historicalFact = "명상록 제5권 첫 절은 아침에 일어나기 싫을 때 인간으로서 해야 할 일을 생각하라고 스스로에게 말합니다. 개인 성찰의 문장이며 매일 특정 시각에 수행했다는 증거는 아닙니다.",
                    modernPractice = "오늘 내가 맡은 일 한 가지를 적고, 몸 상태에 맞는 작은 첫 행동을 정합니다.",
                    source = HistoricalSource(
                        title = "Meditations · Marcus Aurelius · Meric Casaubon 번역",
                        url = "https://www.gutenberg.org/ebooks/2680",
                        locator = "제5권 I · unwilling to rise"
                    ),
                    historicalTime = "아침 · 정확한 시각 미기록"
                ),
                HistoricalRoutine(
                    id = "marcus_aurelius_gratitude",
                    title = "배운 사람 한 명 떠올리기",
                    suggestedTime = "21:00",
                    historicalFact = "명상록 제1권은 가족과 스승 등 여러 사람에게서 배운 태도를 차례로 되짚습니다. 이는 저술에 남은 감사와 배움의 기록입니다.",
                    modernPractice = "오늘 도움을 받은 사람 또는 배움을 준 사람 한 명과 배운 점을 적습니다. 가능하면 짧게 감사의 뜻을 전합니다.",
                    source = HistoricalSource(
                        title = "Meditations · Marcus Aurelius",
                        url = "https://www.gutenberg.org/ebooks/2680",
                        locator = "제1권 · 가족과 스승에게서 배운 점"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "thoreau",
            name = "헨리 데이비드 소로",
            field = "문학 · 철학 · 환경",
            info = "미국 · 1817–1862",
            imageResId = R.drawable.portrait_thoreau,
            bio = "월든에서 생활을 돌아보고 읽기와 자연 관찰을 글로 남겼습니다. 연못 목욕을 그대로 재현하는 대신 안전한 아침 정돈과 깊은 읽기로 적용합니다.",
            portraitCredit = "Benjamin D. Maxham active 1848 - 1858 · Public domain\nhttps://commons.wikimedia.org/wiki/File:Benjamin_D._Maxham_-_Henry_David_Thoreau_-_Restored.jpg\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "thoreau_morning_renewal",
                    title = "아침을 새롭게 시작",
                    suggestedTime = "07:15",
                    historicalFact = "Walden의 Where I Lived, and What I Lived For에서 소로는 일찍 일어나 연못에서 목욕하며 아침을 새로움의 초대로 받아들였다고 회상합니다. 정확한 기상 시각은 제시되지 않습니다.",
                    modernPractice = "세수하고 가볍게 몸을 풀며 창밖의 자연을 잠시 살핍니다. 연못 수영이나 위험한 야외 목욕은 따라 하지 않습니다.",
                    source = HistoricalSource(
                        title = "Walden · Henry David Thoreau",
                        url = "https://www.gutenberg.org/ebooks/205",
                        locator = "Where I Lived, and What I Lived For · Every morning was a cheerful invitation"
                    ),
                    historicalTime = "이른 아침 · 정확한 시각 미기록"
                ),
                HistoricalRoutine(
                    id = "thoreau_deep_reading",
                    title = "한 문단 깊게 읽기",
                    suggestedTime = "19:00",
                    historicalFact = "Walden의 Reading 장은 좋은 글을 깨어 있는 정신과 노력으로 읽을 것을 강조합니다. 정해진 하루 독서 분량을 기록한 시간표가 아니라 독서에 관한 저술입니다.",
                    modernPractice = "읽기 어려운 글 한 문단을 천천히 읽고, 이해한 내용을 자신의 말로 두 문장 씁니다.",
                    source = HistoricalSource(
                        title = "Walden · Henry David Thoreau",
                        url = "https://www.gutenberg.org/ebooks/205",
                        locator = "Reading · read the best that is in literature"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "douglass",
            name = "프레더릭 더글러스",
            field = "문학 · 사회",
            info = "미국 · 1818–1895",
            imageResId = R.drawable.portrait_douglass,
            bio = "노예제 아래에서 읽고 쓰는 법을 배워 자신의 경험을 글과 연설로 알렸습니다. 학습을 막는 현실 속에서 이어 간 읽기와 쓰기를 기록으로 확인합니다.",
            portraitCredit = "George Kendall Warren · Public domain\nhttps://commons.wikimedia.org/wiki/File:Frederick_Douglass_(circa_1879)_(cropped).jpg\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "douglass_learn_with_others",
                    title = "누군가와 함께 한 가지 배우기",
                    suggestedTime = "12:30",
                    historicalFact = "더글러스는 자서전 제7장에서 주변 소년들과 기회를 만들어 읽기를 배웠고, 빵을 나누며 그들에게 배움을 얻었다고 회상합니다. 당시 노예제를 미화하는 성공담이 아니라 억압 속 학습의 기록입니다.",
                    modernPractice = "믿을 수 있는 사람에게 모르는 것 하나를 묻거나 읽은 글에 대해 서로 설명합니다.",
                    source = HistoricalSource(
                        title = "Narrative of the Life of Frederick Douglass, an American Slave",
                        url = "https://www.gutenberg.org/ebooks/23",
                        locator = "Chapter VII · bread of knowledge"
                    ),
                    historicalTime = null
                ),
                HistoricalRoutine(
                    id = "douglass_copy_then_write",
                    title = "따라 쓰고 내 말로 다시 쓰기",
                    suggestedTime = "19:15",
                    historicalFact = "더글러스는 제7장에서 토머스의 필사 연습장에 남은 공간을 활용해 글씨를 따라 쓰며, 오랜 노력 끝에 쓰는 법을 배웠다고 기록합니다.",
                    modernPractice = "짧은 문장 하나를 손으로 쓰고 그 뜻을 자신의 말로 다시 표현합니다.",
                    source = HistoricalSource(
                        title = "Narrative of the Life of Frederick Douglass, an American Slave",
                        url = "https://www.gutenberg.org/ebooks/23",
                        locator = "Chapter VII · Master Thomas’s copy-book"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "john_muir",
            name = "존 뮤어",
            field = "환경 · 문학",
            info = "스코틀랜드·미국 · 1838–1914",
            imageResId = R.drawable.portrait_john_muir,
            bio = "시에라 산맥에서 자연을 관찰하고 노트와 스케치로 남겼습니다. 한 지역의 여행 기록을 평생의 고정 시간표로 일반화하지 않습니다.",
            portraitCredit = "Carlton E. Watkins · Public domain\nhttps://commons.wikimedia.org/wiki/File:John_Muir_Watkins_Portrait_circa_1875_Edit.png\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "john_muir_field_note",
                    title = "야외 관찰 노트 한 줄",
                    suggestedTime = "12:15",
                    historicalFact = "My First Summer in the Sierra의 1869년 6월 3일 기록에서 뮤어는 노트를 허리에 매달고 출발했다고 적습니다. 이후 날짜별로 식물과 풍경의 관찰을 남겼습니다.",
                    modernPractice = "짧게 밖으로 나가 눈에 들어온 생물이나 풍경 하나의 색·형태·장소를 기록합니다.",
                    source = HistoricalSource(
                        title = "My First Summer in the Sierra · John Muir",
                        url = "https://www.gutenberg.org/ebooks/32540",
                        locator = "Chapter I · June 3 · notebook tied to my belt"
                    ),
                    historicalTime = null
                ),
                HistoricalRoutine(
                    id = "john_muir_landscape_sketch",
                    title = "풍경을 멈춰서 그리기",
                    suggestedTime = "17:00",
                    historicalFact = "1869년 6월 5일 기록에서 뮤어는 소나무를 그리고 싶어 했으며 잠시 멈춰 피노블랑코 봉우리를 스케치했다고 적습니다. 여행 중 확인되는 행동을 현대적 실천으로 적용합니다.",
                    modernPractice = "안전한 장소에 서서 나무나 건물의 윤곽을 5분 동안 그립니다. 예쁜 결과보다 실제로 본 특징을 남깁니다.",
                    source = HistoricalSource(
                        title = "My First Summer in the Sierra · John Muir",
                        url = "https://www.gutenberg.org/ebooks/32540",
                        locator = "Chapter I · June 5 · sketch of Pino Blanco peak"
                    ),
                    historicalTime = null
                ),
            )
        ),
        Hero(
            id = "jane_addams",
            name = "제인 애덤스",
            field = "사회 · 교육",
            info = "미국 · 1860–1935",
            imageResId = R.drawable.portrait_jane_addams,
            bio = "헐하우스에서 이웃과 함께 교육과 사회 활동을 이어 간 사회개혁가입니다. 자서전에 남은 이른 독서와 공동 활동의 경험을 실천으로 연결합니다.",
            portraitCredit = "Adam Cuerden · Public domain\nhttps://commons.wikimedia.org/wiki/File:Jane_Addams_-_Bain_News_Service.jpg\n비율 유지·축소·JPEG 변환. ",
            routines = listOf(
                HistoricalRoutine(
                    id = "jane_addams_early_reading",
                    title = "아침에 관심 주제 읽기",
                    suggestedTime = "07:45",
                    historicalFact = "애덤스는 Twenty Years at Hull-House에서 한때 이른 아침에 독서를 시작했으나 계획한 방식이 기대만큼 도움이 되지 않아 읽을 책을 바꾸었다고 회상합니다. 꾸준함뿐 아니라 방법을 조정한 경험입니다.",
                    modernPractice = "관심 있는 주제의 글을 10분 읽고, 지금의 방식이 나에게 맞는지 한 문장으로 돌아봅니다.",
                    source = HistoricalSource(
                        title = "Twenty Years at Hull-House · Jane Addams",
                        url = "https://www.gutenberg.org/ebooks/1325",
                        locator = "Chapter I · did in fact later begin a course of reading in the early morning hours"
                    ),
                    historicalTime = "이른 아침 · 정확한 시각 미기록"
                ),
                HistoricalRoutine(
                    id = "jane_addams_community_learning",
                    title = "이웃과 작은 배움 나누기",
                    suggestedTime = "19:00",
                    historicalFact = "애덤스는 헐하우스의 오전 유치원·오후 아동 모임·저녁 성인 교육 및 사회 모임이 이어져 생활 습관을 조정해야 했다고 기록합니다. 공동체 활동을 위한 일정의 변화가 드러나는 대목입니다.",
                    modernPractice = "가족·이웃·동료 한 명과 배운 내용을 나누거나 함께 할 작은 도움을 정합니다. 참여 가능한 모임을 알아보는 것도 좋습니다.",
                    source = HistoricalSource(
                        title = "Twenty Years at Hull-House · Jane Addams",
                        url = "https://www.gutenberg.org/ebooks/1325",
                        locator = "Chapter VII · Some Early Undertakings at Hull-House · educational and social organizations of adults"
                    ),
                    historicalTime = null
                ),
            )
        ),
    )

    val all: List<Hero> = original + NewHeroCatalog.all

    val default: Hero = all.first()

    fun getOrDefault(id: String?): Hero = all.firstOrNull { it.id == id } ?: default

    fun filterByField(field: String?): List<Hero> = when (field) {
        null, ALL_FIELDS -> all
        else -> all.filter { hero -> hero.field.split('·').any { it.trim() == field } }
    }
}
