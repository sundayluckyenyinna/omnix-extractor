package com.accionmfb.omnix.extractor.modules.cgap;

import com.accionmfb.omnix.extractor.modules.cgap.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
//@Repository
@Transactional
public class CgapRepository {

    @PersistenceContext
    private EntityManager em;

    public AppUser getAppUserUsingUsername(String username){
        return em.createQuery("select a from AppUser a where a.username = :username", AppUser.class)
                .setParameter("username", username)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Branch getBranchUsingBranchCode(String branchCode){
        return em.createQuery("select b from Branch b where b.branchCode = :branchCode", Branch.class)
                .setParameter("branchCode", branchCode)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public BVN getBVNUsingBvnNumber(String bvnNumber){
        return em.createQuery("select b from BVN b where b.customerBvn = :bvn", BVN.class)
                .setParameter("bvn", bvnNumber)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Product getProductByCategoryCode(String categoryCode){
        return em.createQuery("select p from Product p where p.categoryCode = :categoryCode", Product.class)
                .setParameter("categoryCode", categoryCode)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Account createAccount(Account account){
        em.persist(account);
        em.flush();
        return account;
    }

    public Customer findCustomerByMobileNumber(String mobileNumber){
        return em.createQuery("select c from Customer c where c.mobileNumber = :mobileNumber", Customer.class)
                .setParameter("mobileNumber", mobileNumber)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Customer findCustomerByCustomerNumber(String customerNumber){
        return em.createQuery("select c from Customer c where c.customerNumber = :customerNumber", Customer.class)
                .setParameter("customerNumber", customerNumber)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<String> getAllCustomerAccountNumbers(Customer customer){
        return em.createQuery("select a from Account a where a.customer = :customer", Account.class)
                .setParameter("customer", customer)
                .getResultList()
                .stream()
                .filter(Objects::nonNull)
                .map(account -> ProcessLoanTemplate.valueOrDefault(account.getAccountNumber(), account.getOldAccountNumber()))
                .collect(Collectors.toList());
    }

    public Customer createCustomer(Customer customer){
        em.persist(customer);
        em.flush();
        return customer;
    }

    public RubyxLoanRenewal createRubyxLoanRenewalRecord(RubyxLoanRenewal rubyxLoanRenewal){
        em.persist(rubyxLoanRenewal);
        em.flush();
        return rubyxLoanRenewal;
    }

    public List<ExcelRecordTracker> getAllPendingExcelRecords(){
        return em.createQuery("select ex from ExcelRecordTracker ex where ex.status = :status", ExcelRecordTracker.class)
                .setParameter("status", "PENDING")
                .getResultList();
    }

    public Long getCountOfExcelRecordTracker(){
        return em.createQuery("select count(ex) from ExcelRecordTracker ex", Long.class).getSingleResult();
    }

    public ExcelRecordTracker saveExcelRecordTracker(ExcelRecordTracker tracker){
        em.persist(tracker);
        em.flush();
        return tracker;
    }

    public ExcelRecordTracker updatedExcelRecordTracker(ExcelRecordTracker tracker){
        em.merge(tracker);
        em.flush();
        return tracker;
    }

    public List<RubyxLoanRenewal> getRubyxLoanRenewals(LocalDateTime startDate){
        return em.createQuery("select r from RubyxLoanRenewal r where r.createdAt >= :startDate", RubyxLoanRenewal.class)
                .setParameter("startDate", startDate)
                .getResultList();
    }

    public List<ExcelRecordTracker> getAllRecordTracker(){
        return em.createQuery("select e from ExcelRecordTracker e", ExcelRecordTracker.class)
                .getResultList();
    }

    public void deleteAllTrackers(){
        List<ExcelRecordTracker> trackers = getAllRecordTracker();
        for(ExcelRecordTracker tracker : trackers){
            em.remove(em.contains(tracker) ? tracker : em.merge(tracker));
            em.flush();
        }
    }

    public AppUser getAppUserUsingChannel(String channel){
        return em.createQuery("select a from AppUser a where a.channel = :channel", AppUser.class)
                .setParameter("channel", channel)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Account getAccountUsingAccountNumber(String accountNumber){
        return em.createQuery("select a from Account a where a.accountNumber = :accountNumber or a.oldAccountNumber = :accountNumber", Account.class)
                .setParameter("accountNumber", accountNumber)
                .getResultList()
                .stream().findFirst()
                .orElse(null);
    }

    public void deleteRuvbyxRecord(Long id){
        RubyxLoanRenewal renewal = em.createQuery("select r from RubyxLoanRenewal r where r.id = :id", RubyxLoanRenewal.class)
                .setParameter("id", id)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
        if(Objects.nonNull(renewal)){
            em.remove(em.contains(renewal) ? renewal : em.merge(renewal));
            em.flush();
            log.info("Rubyx record with ID: {} removed successfully", id);
        }
    }

    public ExcelWorkbookRecord saveExcelWorkbookRecord(ExcelWorkbookRecord excelWorkbookRecord){
        em.persist(excelWorkbookRecord);
        em.flush();
        return excelWorkbookRecord;
    }

    public ExcelWorkbookRecord updateExcelWorkbookRecord(ExcelWorkbookRecord record){
        em.merge(record);
        em.flush();
        return record;
    }

    public List<ExcelWorkbookRecord> getAllWorkbookRecords(){
        return em.createQuery("select wr from ExcelWorkbookRecord wr", ExcelWorkbookRecord.class)
                .getResultList();
    }

    public boolean deleteExcelWorkbookRecord(String workbookName){
        ExcelWorkbookRecord record = getExcelWorkbookRecordByName(workbookName);
        if(Objects.nonNull(record)) {
            em.remove(em.contains(record) ? record : em.merge(record));
            em.flush();
            return true;
        }
        return false;
    }


    public ExcelWorkbookRecord getExcelWorkbookRecordByName(String name){
        return em.createQuery("select ex from ExcelWorkbookRecord ex where ex.workbookName = :name", ExcelWorkbookRecord.class)
                .setParameter("name", name)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Account getAccountHavingAccountNumberIn(List<String> accountNumbers){
        return em.createQuery("select a from Account a where a.accountNumber in (:list) or a.oldAccountNumber in (:list)", Account.class)
                .setParameter("list", accountNumbers)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Account updateAccountRecord(Account account){
        em.merge(account);
        em.flush();
        return account;
    }

    public RubyxLoanRenewal getRubyxLoanByCustomerNumberAndProductCode(String customerNumber, String productCode){
        return em.createQuery("select ru from RubyxLoanRenewal ru where ru.customerNumber = :customerNumber and ru.productCode = :productCode", RubyxLoanRenewal.class)
                .setParameter("customerNumber", customerNumber)
                .setParameter("productCode", productCode)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public RubyxLoanRenewal updateRubyxLoanRenewal(RubyxLoanRenewal rubyxLoanRenewal){
        em.merge(rubyxLoanRenewal);
        em.flush();
        return rubyxLoanRenewal;
    }

    public FailedExcelCgapRecord saveFailedExcelRecords(FailedExcelCgapRecord failedExcelCgapRecord){
        em.persist(failedExcelCgapRecord);
        em.flush();
        return failedExcelCgapRecord;
    }

    public FailedExcelCgapRecord updateFailedExcelRecord(FailedExcelCgapRecord failedExcelCgapRecord){
        em.merge(failedExcelCgapRecord);
        em.flush();
        return failedExcelCgapRecord;
    }

    public List<FailedExcelCgapRecord> getAllFailedExcelRecords(){
        return em.createQuery("select f from FailedExcelCgapRecord f", FailedExcelCgapRecord.class)
                .getResultList();
    }
}
