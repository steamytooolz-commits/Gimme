package com.example.domain

object AiActionLexicon {
    val FULL_LEXICON = """
        [SUPPORTED AI ACTIONS - Over 150+ Action Types]
        Use the 'simulate_action(action_type, outcome_description)' tool to integrate these mechanics seamlessly across the simulation. 
        Select an appropriate action_type string from the lists below or invent related ones if needed.
        
        Investigation Actions:
        forensic_dusting, luminal_spray, analyze_blood_spatter, fingerprint_match, dna_extraction, toxicology_screen, 
        ballistics_report, autopsy_incision, rigor_mortis_check, lividity_analysis, entomology_swab, dental_records_check, 
        fiber_analysis, soil_sample_test, chemical_reagent_test, uv_light_scan, track_footprints, tire_tread_analysis,
        document_forgery_check, handwriting_analysis, trace_metal_detection, gunshot_residue_test,
        
        Digital Forensics:
        decrypt_hard_drive, extract_phone_records, ping_cell_tower, recover_deleted_files, analyze_metadata, 
        bypass_encryption, wiretap_network, trace_ip_address, cctv_enhancement, audio_filtering, facial_recognition_scan,
        
        Interrogation Tactics:
        good_cop_routine, bad_cop_routine, bluff_suspect, present_contradiction, pressure_alibi, sympathetic_approach, 
        hostile_confrontation, read_miranda_rights, polygraph_test, psychological_profiling, stress_induction, 
        offer_plea_deal, threaten_max_sentence, exploit_phobia, leverage_family_ties,
        
        Legal & Bureaucratic:
        issue_subpoena, request_warrant, file_injunction, motion_to_compel, seal_records, unseal_records, 
        bribe_clerk, leak_to_press, gag_order_request, grand_jury_indictment, habeas_corpus_petition,
        extradition_request, review_tax_returns, audit_financials, freeze_bank_assets,
        
        Field Operations:
        tail_suspect, stakeout_location, plant_bug, wire_informant, breach_door, stealth_entry, 
        undercover_infiltration, flashbang_deploy, secure_perimeter, call_backup, pursuit_suspect,
        k9_unit_search, drone_surveillance, thermal_imaging_scan,
        
        Courtroom Tactics:
        cross_examine, redirect_examination, object_hearsay, object_speculation, object_leading, 
        object_badgering, impeach_witness, enter_exhibit, rest_case, closing_argument, opening_statement,
        poll_jury, request_mistrial, approach_bench, sidebar_conference,
        
        Medical & Psychological:
        administer_truth_serum, psychiatric_eval, prescribe_sedative, trauma_counseling,
        analyze_toxins, run_mri, genetic_sequencing,
        
        Underworld & Covert:
        contact_fence, hire_informant, pay_bribe, forge_documents, smuggle_contraband, 
        intimidate_witness, blackmail_official, decode_cipher, find_safehouse,
        
        ... and 60+ more variations. You are fully empowered to simulate any of these 150+ actions using the 'simulate_action' tool!
    """.trimIndent()
}
