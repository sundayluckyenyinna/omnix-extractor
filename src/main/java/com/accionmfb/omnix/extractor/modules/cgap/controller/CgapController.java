package com.accionmfb.omnix.extractor.modules.cgap.controller;

import com.accionmfb.omnix.extractor.modules.cgap.CgapRepository;
import com.accionmfb.omnix.extractor.modules.cgap.ProcessLoanTemplate;
import com.accionmfb.omnix.extractor.modules.cgap.models.ExcelRecordTracker;
import com.accionmfb.omnix.extractor.modules.cgap.models.ExcelWorkbookRecord;
import com.accionmfb.omnix.extractor.modules.cgap.models.FailedExcelCgapRecord;
import com.accionmfb.omnix.extractor.modules.cgap.models.RubyxLoanRenewal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
//@Controller
@RequiredArgsConstructor
public class CgapController {

    private final CgapRepository cgapRepository;
    private final ProcessLoanTemplate processLoanTemplate;

    @GetMapping(value = "/")
    public String homePage(Model model){
        List<RubyxLoanRenewal> rubyxLoanRenewals = cgapRepository.getRubyxLoanRenewals(LocalDateTime.now().minusDays(2));
        List<ExcelRecordTracker> trackers = cgapRepository.getAllRecordTracker();
        List<ExcelWorkbookRecord> records = cgapRepository.getAllWorkbookRecords();
        List<FailedExcelCgapRecord> failedExcelCgapRecords = cgapRepository.getAllFailedExcelRecords();
        model.addAttribute("renewals", rubyxLoanRenewals);
        model.addAttribute("trackers", trackers);
        model.addAttribute("workbooks", records);
        model.addAttribute("failed", failedExcelCgapRecords);
        return "index_page";
    }

    @GetMapping(value = "/start")
    public String startProcessing() throws IOException {

        CompletableFuture.runAsync(() -> {
            try {
                processLoanTemplate.processLoanTemplate();
            } catch (IOException e) {e.printStackTrace();}
        }).exceptionally((throwable -> {
            throwable.printStackTrace();
            return null;
        }));
        return "redirect:/";
    }

    @GetMapping(value = "/delete-all-excel-record-tracker")
    public String deleteAllExcelTracker(){
        cgapRepository.deleteAllTrackers();
        return "redirect:/";
    }

    @GetMapping(value = "/delete-rubyx-record/{id}")
    public String deleteRubyxRecord(@PathVariable("id") String id){
        cgapRepository.deleteRuvbyxRecord(Long.parseLong(id));
        return "redirect:/";
    }

    @GetMapping(value = "/excel-record/delete/{workbookName}")
    public String deleteWorkbookByName(@PathVariable("workbookName") String workbookName){
        boolean deleted = cgapRepository.deleteExcelWorkbookRecord(workbookName);
        System.out.printf("Excel record deleted: %s", deleted);
        return "redirect:/";
    }

}
