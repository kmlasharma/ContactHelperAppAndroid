package com.synchronoss.androidDev.contactcreaterapp;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.Intent;
import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests responsible
 * for importing and deleting contacts.
 * <p/>
 */
public class CreateAndAddContacts extends IntentService {
    private static final String TAG = "CONTACT_HELPER";
    private static final String ACTION_IMPORT = MainActivity.MAIN_ACTIVITY + ".action.IMPORT";
    private static final String CREATION_KEY = "CREATION";
    private static final String CSV_KEY = "CSV";
    private static int firstnameIndex;
    private static int lastnameIndex;
    private static int websiteIndex;
    private static int noteIndex;
    private static int emailIndex;
    private static int phoneIndex;

    /**
     * Starts this service to perform action Import. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startActionImport(Context context) {
        Intent intent = new Intent(context, CreateAndAddContacts.class);
        intent.setAction(ACTION_IMPORT);
        intent.putExtra(CREATION_KEY, "10");
        context.startService(intent);
    }

    public CreateAndAddContacts() {
        super("CreateAndAddContacts");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_IMPORT.equals(action)) {
                Log.d(TAG, "Action import");
                Bundle extras = intent.getExtras();
                if (extras != null && extras.containsKey(CREATION_KEY)) {
                    int num = Integer.parseInt(extras.getString(CREATION_KEY));
                    generateContacts(num);
                } else if (extras != null && extras.containsKey(CSV_KEY)) {
                    String csvFile = extras.getString(CSV_KEY);
                    importContactsFromCSV(csvFile);
                }
            } else {
                Log.d(TAG, "Unsupported action: " + action);
            }
        }
    }

    public void importContactsFromCSV(String filename) {
        Log.d(TAG, "Reading contacts in from " + filename);
//        String csvFile = filename.substring(filename.lastIndexOf('/') + 1);
//        if (!csvFile.endsWith(".vcf")) {
//            csvFile += ".vcf";
//        }
        File csv = new File(filename);
        csv = new File(csv.getAbsolutePath());
        if (csv.exists()) {
            Log.d(TAG, "The csv file exists");
        }
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {
            br = new BufferedReader(new FileReader(csv));
            String labelLine = br.readLine(); //read in first line
            String labels[] = labelLine.split(cvsSplitBy);
            String field, surname;
            whatsSupported(labels);

            while ((line = br.readLine()) != null) {
                String contactFields[] = line.split(cvsSplitBy);
                boolean nameEntered = false;
                ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
                operationList.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                        .build());
                for (int i = 0; i < contactFields.length; i++) {
                    field = contactFields[i];
                    if (i == firstnameIndex || i == lastnameIndex) {
                        if (!nameEntered) {
                            nameEntered = true;
                            surname = contactFields[i+1];
                            // first and last names
                            operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, field)
                                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, surname)
                                    .build());
                        }
                    } else if (i == phoneIndex) {
                        //phone number
                        operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, field)
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                                .build());
                    } else if (i == emailIndex) {
                        //email
                        operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.DATA, field)
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                                .build());
                    } else if (i == noteIndex) {
                        //notes
                        operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Note.NOTE, field)
                                .build());
                    } else if (i == websiteIndex) {
                        //website
                        operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Website.URL, field)
                                .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE)
                                .build());
                    }

                }
                try{
                    ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }

        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found exception");
            e.printStackTrace();
        } catch (IOException e) {
            Log.d(TAG, "IOException");
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                    Log.d(TAG, "Closing BR");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "Finished reading in CSV file.");
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void whatsSupported(String labels[]) {
        String field;
        for (int i = 0; i < labels.length; i++) {
            field = labels[i].toLowerCase();
            if (field.contains("first") || field.contains("given")) {
                Log.d(TAG, "First name is included");
                firstnameIndex = i;
            } else if (field.contains("last") || field.contains("sur") || field.contains("family")) {
                Log.d(TAG, "Last name is included");
                lastnameIndex = i;
            } else if (field.contains("url") || field.contains("site")) {
                Log.d(TAG, "URL is included");
                websiteIndex = i;
            } else if (field.contains("note")) {
                Log.d(TAG, "Note is included");
                noteIndex = i;
            } else if (field.contains("mail")) {
                Log.d(TAG, "Mail is included");
                emailIndex = i;
            } else if (field.contains("phone") || field.contains("num")) {
                Log.d(TAG, "Phone no is included");
                phoneIndex = i;
            } else {
                Log.d(TAG, "The field: " + field + " is not supported/not recognised.");
            }

        }
    }

    public void generateContacts(int amount) {
        Log.d(TAG, "Creating " + amount + " contacts...");
        for (int i = 0; i < amount; i++) {
            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
            operationList.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            // first and last names
            operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, String.format("FirstName%03d", i))
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME, String.format("SecondName%03d", i))
                    .build());

            //phone number
            operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, "09876543210")
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                    .build());

            //email
            operationList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE,ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.DATA, "abc@xyz.com")
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                    .build());


            try{
                ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        Log.d(TAG, "Finished contact import");
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
