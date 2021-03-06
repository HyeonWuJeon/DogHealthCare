package com.example.demo.diagnosis.controller;

import com.example.demo.symptom.domain.Symptom;
import com.example.demo.symptom.dto.SymptomForm;
import com.example.demo.symptom.repository.SymptomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@Slf4j
public class DiagnosisApiController {
    private final SymptomRepository symptomRepository;

    /**
     * FUNCTION :: AI 모델 변경
     *
     * @param form
     * @param model
     * @return
     */
    @PostMapping("/api/AIRemodeling")
    public void variable(@Valid SymptomForm form, Model model) {

        //LINE :: 증상을 추가한다.
        symptomRepository.save(Symptom.builder()
                .name(form.getModel())
                .build());

        RestTemplate restTemplate = new RestTemplate();

        //LINE :: FLASK 서버와 연결
//        String url = "http://15.165.169.119:5000/reset";
        String url = "http://localhost:80/reset";

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();

        parameters.add("add", form.getModel()); // LINE :: 증상
        parameters.add("random_state", form.getRandom_state()); // LINE :: 가지수
        parameters.add("test_size", form.getTest_size()); // LINE :: 테스트 사이즈

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, parameters, String.class); // LINE :: 파라메타 전송

    }
}