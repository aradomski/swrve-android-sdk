package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveSDKBase;
import com.swrve.sdk.common.R;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.ActionBehaviours;
import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.ChoiceInputResponse;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ControlActions;
import com.swrve.sdk.conversations.engine.model.ControlBase;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.ConversationReply;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.conversations.engine.model.DateChoice;
import com.swrve.sdk.conversations.engine.model.DateSaver;
import com.swrve.sdk.conversations.engine.model.InputBase;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.MultiValueLongInput;
import com.swrve.sdk.conversations.engine.model.NPSInput;
import com.swrve.sdk.conversations.engine.model.OnContentChangedListener;
import com.swrve.sdk.conversations.engine.model.TextInput;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class ConversationFragment extends Fragment implements OnClickListener {
    private static final String LOG_TAG = "ConversationFragment";

    private ViewGroup root;
    private LinearLayout contentLayout;
    private LinearLayout controlLayout;
    private Toolbar toolbar;
    private ValidationDialog validationDialog;
    private SwrveConversation swrveConversation;
    private ConversationPage page;
    private SwrveBase controller;
    private ArrayList<ConversationInput> inputs;
    private HashMap<String, UserInputResult> userInteractionData;
    private boolean userInputValid = false;


    public ConversationPage getPage() {
        return page;
    }

    public void setPage(ConversationPage page) {
        this.page = page;
    }

    public ArrayList<ConversationInput> getInputs() {
        return inputs;
    }

    public void setInputs(ArrayList<ConversationInput> inputs) {
        this.inputs = inputs;
    }

    public HashMap<String, UserInputResult> getUserInteractionData() {
        return userInteractionData;
    }

    public void setUserInteractionData(HashMap<String, UserInputResult> userInteractionData) {
        this.userInteractionData = userInteractionData;
    }

    public static ConversationFragment create(SwrveConversation swrveConversation) {
        ConversationFragment f = new ConversationFragment();
        // TODO: STM Beware of OnResumes and other state held things.
        f.swrveConversation = swrveConversation;
        f.controller = (SwrveBase) SwrveSDKBase.getInstance();
        return f;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        inputs = (inputs == null) ? new ArrayList<ConversationInput>() : inputs;
        userInteractionData = (userInteractionData == null) ? new HashMap<String, UserInputResult>() : userInteractionData;

        if (page != null) {
            View currentView = getView();
            openConversationOnPage(page);
            // Populate the page with existing inputs and answers
            for (String key : userInteractionData.keySet()) {
                UserInputResult userInput = userInteractionData.get(key);
                String fragmentTag = userInput.getFragmentTag();
                View inputView = currentView.findViewWithTag(fragmentTag);
                if (userInput.isSingleChoice()) {
                    MultiValueInputControl inputControl = (MultiValueInputControl) inputView;
                    inputControl.setUserInput(userInput);
                } else if (userInput.isMultiChoice()) {
                    MultiValueLongInputControl inputControl = (MultiValueLongInputControl) inputView;
                    inputControl.setUserInput(userInput);
                } else if (userInput.isNps()) {
                    NPSlider inputControl = (NPSlider) inputView;
                    inputControl.setUserInput(userInput);
                } else if (userInput.isTextInput()) {
                    EditTextControl inputControl = (EditTextControl) inputView;
                    inputControl.setUserInput(userInput);
                }
            }
        } else {
            openFirstPage();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cio__conversation_fragment, container, false);
    }


    public void openFirstPage() {
        page = swrveConversation.getFirstPage();
        sendStartNavigationEvent(page.getTag());
        openConversationOnPage(page);
    }

    @SuppressLint("NewApi")
    public void openConversationOnPage(ConversationPage conversationPage) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        Context context = getActivity().getApplicationContext();
        this.page = conversationPage;
        if (inputs.size() > 0) {
            inputs.clear();
        }

        Activity activity = getActivity();
        activity.setTitle(conversationPage.getTitle());

        root = (ViewGroup) getView();
        if (root == null) {
            return;
        }

        toolbar = (Toolbar) root.findViewById(R.id.cio__toolbar);
        if(toolbar != null){
            // Background Color
            int actionBarBGColor = page.getHeaderBackgroundColor(context);
            ColorDrawable actionBarBG = new ColorDrawable(actionBarBGColor);
            if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
                toolbar.setBackgroundDrawable(actionBarBG);
            } else {
                toolbar.setBackground(actionBarBG);
            }

            // Title
            int actionBarTextColor = page.getHeaderBackgroundTextColor(context);
            toolbar.setTitleTextColor(actionBarTextColor);
            toolbar.setTitle(page.getTitle());
            toolbar.setLogo(page.getHeaderIcon(context));
        }

        contentLayout = (LinearLayout) root.findViewById(R.id.cio__content);
        controlLayout = (LinearLayout) root.findViewById(R.id.cio__controls);

        if (contentLayout.getChildCount() > 0) {
            contentLayout.removeAllViews();
        }

        if (controlLayout.getChildCount() > 0) {
            controlLayout.removeAllViews();
        }

        LayoutParams controlLp;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            controlLp = new LayoutParams(root.getLayoutParams());
        } else {
            controlLp = new LayoutParams(root.getLayoutParams().width, root.getLayoutParams().height);
        }

        LayoutParams contentLp;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            contentLp = new LayoutParams(root.getLayoutParams());
        } else {
            contentLp = new LayoutParams(root.getLayoutParams().width, root.getLayoutParams().height);
        }

        controlLp.height = LayoutParams.WRAP_CONTENT;
        contentLp.weight = 1;

        TypedArray margins = getActivity().getTheme().obtainStyledAttributes(new int[] {R.attr.conversationControlLayoutMargin});
        int controlLayoutMarginInPixels = margins.getDimensionPixelSize(0, 0);
        // Set the background from whatever color the page object specifies as well as the control tray down the bottom
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            contentLayout.setBackgroundDrawable(page.getContentBackgroundDrawable(context));
            controlLayout.setBackgroundDrawable(page.getControlTrayBackgroundDrawable(context));
        } else {
            contentLayout.setBackground(page.getContentBackgroundDrawable(context));
            controlLayout.setBackground(page.getControlTrayBackgroundDrawable(context));
        }

        // Now setup the controls
        int primaryButtonColor = page.getPrimaryButtonColor(context);
        int primaryButtonTextColor = page.getPrimaryButtonTextColor(context);
        int secondaryButtonColor = page.getSecondaryButtonColor(context);
        int secondaryButtonTextColor = page.getSecondaryButtonTextColor(context);
        int neutralButtonColor = page.getNeutralButtonColor(context);
        int neutralButtonTextColor = page.getNeutralButtonTextColor(context);

        int numControls = conversationPage.getControls().size();
        if (numControls == 0) {
            ConversationPageException exception = new ConversationPageException("No controls were detected in this page. This is a bad conversation!");
            sendErrorNavigationEvent(page.getTag(), exception); // No exception. We just couldn't find a page attached to the control.

            ConversationButton ctrlConversationButton = new ConversationButton(activity, null, R.attr.conversationControlSecondaryButtonStyle);
            ctrlConversationButton.setText("Done");



        // Set the background from whatever color the page object specifies as well as the control tray down the bottom
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            contentLayout.setBackgroundDrawable(page.getContentBackgroundDrawable(context));
            controlLayout.setBackgroundDrawable(page.getControlTrayBackgroundDrawable(context));
        } else {
            contentLayout.setBackground(page.getContentBackgroundDrawable(context));
            controlLayout.setBackground(page.getControlTrayBackgroundDrawable(context));
        }

            LayoutParams buttonLP;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                buttonLP = new LayoutParams(controlLp);
            } else {
                buttonLP = new LayoutParams(controlLp.width, controlLp.height);
            }
            buttonLP.weight = 1;
            buttonLP.leftMargin = controlLayoutMarginInPixels;
            buttonLP.rightMargin = controlLayoutMarginInPixels;
            buttonLP.topMargin = controlLayoutMarginInPixels;
            buttonLP.bottomMargin = controlLayoutMarginInPixels;

            ctrlConversationButton.setLayoutParams(buttonLP);
            controlLayout.addView(ctrlConversationButton);
            ctrlConversationButton.setConversationButtonColor(primaryButtonColor);
            ctrlConversationButton.setConversationButtonTextColor(primaryButtonTextColor);
            ctrlConversationButton.setCurved();
            ctrlConversationButton.setLayoutParams(buttonLP);
            controlLayout.addView(ctrlConversationButton);
            ctrlConversationButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(LOG_TAG, "User recieved a bad conversation and had to finish the conversation prematurely");
                    sendDoneNavigationEvent(page.getTag(), "no control present");
                    getActivity().finish();
                }
            });
            ctrlConversationButton.setConversationButtonColor(primaryButtonColor);
            ctrlConversationButton.setConversationButtonTextColor(primaryButtonTextColor);
            ctrlConversationButton.setCurved();
        }

        for (int i = 0; i < numControls; i++) {
            ConversationAtom atom = conversationPage.getControls().get(i);

            boolean isFirst = (i == 0);
            boolean isLast = (i == numControls - 1);

            if (atom instanceof ButtonControl) {
                // There are times when the layout or styles will need to change
                // based on the number of controls.
                // EG if there is one button, make it green. If there are 2
                // buttons, make the first red, and the second green

                ButtonControl ctrl = (ButtonControl) atom;
                ConversationButton ctrlConversationButton = null;

                if (isFirst) {
                    if (numControls == 1) {
                        // Button should be green
                        ctrlConversationButton = new ConversationButton(activity, ctrl, R.attr.conversationControlPrimaryButtonStyle);
                        ctrlConversationButton.setConversationButtonColor(primaryButtonColor);
                        ctrlConversationButton.setConversationButtonTextColor(primaryButtonTextColor);
                    } else if (numControls == 2) {
                        // Button should be red
                        ctrlConversationButton = new ConversationButton(activity, ctrl, R.attr.conversationControlSecondaryButtonStyle);
                        ctrlConversationButton.setConversationButtonColor(secondaryButtonColor);
                        ctrlConversationButton.setConversationButtonTextColor(secondaryButtonTextColor);
                    } else if (numControls > 2) {
                        // Button should be red
                        ctrlConversationButton = new ConversationButton(activity, ctrl, R.attr.conversationControlSecondaryButtonStyle);
                        ctrlConversationButton.setConversationButtonColor(secondaryButtonColor);
                        ctrlConversationButton.setConversationButtonTextColor(secondaryButtonTextColor);
                    }
                } else if (!isFirst && !isLast) {
                    if (numControls == 1) {
                        // Button should be green
                        ctrlConversationButton = new ConversationButton(activity, ctrl, R.attr.conversationControlPrimaryButtonStyle);
                        ctrlConversationButton.setConversationButtonColor(primaryButtonColor);
                        ctrlConversationButton.setConversationButtonTextColor(primaryButtonTextColor);
                    } else if (numControls == 2) {
                        // Button should be green
                        ctrlConversationButton = new ConversationButton(activity, ctrl, R.attr.conversationControlPrimaryButtonStyle);
                        ctrlConversationButton.setConversationButtonColor(primaryButtonColor);
                        ctrlConversationButton.setConversationButtonTextColor(primaryButtonTextColor);
                    } else if (numControls > 2) {
                        // Button should be gray
                        ctrlConversationButton = new ConversationButton(activity, ctrl, R.attr.conversationControlNeutralButtonStyle);
                        ctrlConversationButton.setConversationButtonColor(neutralButtonColor);
                        ctrlConversationButton.setConversationButtonTextColor(neutralButtonTextColor);
                    }
                    // If it is not the first button but is also not the last IE
                    // it is in the middle
                } else if (isLast) {
                    if (numControls == 1) {
                        // Should be green
                        ctrlConversationButton = new ConversationButton(activity, ctrl, R.attr.conversationControlPrimaryButtonStyle);
                        ctrlConversationButton.setConversationButtonColor(primaryButtonColor);
                        ctrlConversationButton.setConversationButtonTextColor(primaryButtonTextColor);
                    } else if (numControls == 2) {
                        // Should be green
                        ctrlConversationButton = new ConversationButton(activity, ctrl, R.attr.conversationControlPrimaryButtonStyle);
                        ctrlConversationButton.setConversationButtonColor(primaryButtonColor);
                        ctrlConversationButton.setConversationButtonTextColor(primaryButtonTextColor);
                    } else if (numControls > 2) {
                        // Should be green
                        ctrlConversationButton = new ConversationButton(activity, ctrl, R.attr.conversationControlPrimaryButtonStyle);
                        ctrlConversationButton.setConversationButtonColor(primaryButtonColor);
                        ctrlConversationButton.setConversationButtonTextColor(primaryButtonTextColor);
                    }
                    // End Button
                }
                // All buttons curved by default on Android.
                ctrlConversationButton.setCurved();

                LayoutParams buttonLP;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    buttonLP = new LayoutParams(controlLp);
                } else {
                    buttonLP = new LayoutParams(controlLp.width, controlLp.height);
                }
                buttonLP.weight = 1;
                buttonLP.leftMargin = (isFirst ? controlLayoutMarginInPixels : controlLayoutMarginInPixels / 2);
                buttonLP.rightMargin = (isLast ? controlLayoutMarginInPixels : controlLayoutMarginInPixels / 2);
                buttonLP.topMargin = controlLayoutMarginInPixels;
                buttonLP.bottomMargin = controlLayoutMarginInPixels;

                ctrlConversationButton.setLayoutParams(buttonLP);
                controlLayout.addView(ctrlConversationButton);
                ctrlConversationButton.setOnClickListener(this);

            } else if (atom instanceof DateChoice) {
                DatePickerButton btn = new DatePickerButton(activity, (DateChoice) atom);

                LayoutParams buttonLP;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    buttonLP = new LayoutParams(controlLp);
                } else {
                    buttonLP = new LayoutParams(controlLp.width, controlLp.height);
                }
                buttonLP.weight = 1;
                buttonLP.leftMargin = (isFirst ? controlLayoutMarginInPixels : controlLayoutMarginInPixels / 2);
                buttonLP.rightMargin = (isLast ? controlLayoutMarginInPixels : controlLayoutMarginInPixels / 2);
                buttonLP.topMargin = controlLayoutMarginInPixels;
                buttonLP.bottomMargin = controlLayoutMarginInPixels;

                btn.setLayoutParams(buttonLP);
                controlLayout.addView(btn);
                btn.setOnClickListener(this);

            } else if (atom instanceof DateSaver) {

            }

        }

        for (ConversationAtom content : conversationPage.getContent()) {
            if (content instanceof Content) {
                Content modelContent = (Content) content;
                if (modelContent.getType().toString().equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_IMAGE)) {
                    ImageView iv = new ImageView(activity, modelContent);
                    String filePath = swrveConversation.getCacheDir().getAbsolutePath() + "/" + modelContent.getValue();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    iv.setTag(content.getTag());
                    iv.setImageBitmap(bitmap);
                    iv.setAdjustViewBounds(true);
                    iv.setScaleType(ScaleType.FIT_CENTER);
                    iv.setPadding(12, 12, 12, 12);
                    contentLayout.addView(iv);
                } else if (modelContent.getType().toString().equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_HTML)) {
                    LayoutParams tvLP;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        tvLP = new LayoutParams(controlLp);
                    } else {
                        tvLP = new LayoutParams(controlLp.width, controlLp.height);
                    }

                    tvLP.width = LayoutParams.MATCH_PARENT;
                    tvLP.height = LayoutParams.WRAP_CONTENT;

                    HtmlSnippetView view = new HtmlSnippetView(activity, modelContent);
                    view.setTag(content.getTag());
                    view.setBackgroundColor(0);
                    view.setLayoutParams(tvLP);
                    contentLayout.addView(view);
                } else if (modelContent.getType().toString().equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_VIDEO)) {
                    LayoutParams tvLP;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        tvLP = new LayoutParams(controlLp);
                    } else {
                        tvLP = new LayoutParams(controlLp.width, controlLp.height);
                    }
                    tvLP.width = LayoutParams.MATCH_PARENT;
                    tvLP.height = LayoutParams.WRAP_CONTENT;

                    HtmlVideoView view = new HtmlVideoView(activity, modelContent);
                    view.setTag(content.getTag());
                    view.setBackgroundColor(0);
                    view.setLayoutParams(tvLP);

                    // Let the eventListener know that something has happened to the video
                    final HtmlVideoView cloneView = view;
                    final String tag = content.getTag();
                    view.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            // TODO: STM Due the fact that we render video in HTML, its very difficult to detect when a video has started/stopped  playing. For now all we can say is that the video was touched. Note that on click listeners behave strange with WebViews
                            stashVideoViewed(page.getTag(), tag, cloneView);
                            return false;
                        }
                    });
                    contentLayout.addView(view);
                } else {
                    TextView tv = new TextView(activity, modelContent, R.attr.conversationTextContentDefaultStyle);
                    tv.setTag(content.getTag());
                    LayoutParams tvLP;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        tvLP = new LayoutParams(controlLp);
                    } else {
                        tvLP = new LayoutParams(controlLp.width, controlLp.height);
                    }
                    tv.setLayoutParams(tvLP);

                    contentLayout.addView(tv);
                }
            } else if (content instanceof InputBase) {
                if (content instanceof TextInput) {
                    // Do stuff for text
                    TextInput inputModel = (TextInput) content;

                    EditTextControl etc = (EditTextControl) getLayoutInflater(null).inflate(R.layout.cio__edittext_input, contentLayout, false);
                    etc.setTag(content.getTag());
                    etc.setModel(inputModel);

                    // Store the result of the content for processing later
                    final EditTextControl etcReference = etc;
                    final String tag = content.getTag();
                    etc.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            HashMap<String, Object> result = new HashMap<String, Object>();
                            etcReference.onReplyDataRequired(result);
                            stashEditTextControlInputData(page.getTag(), tag, result);
                        }
                    });

                    contentLayout.addView(etc);
                    inputs.add(etc);
                } else if (content instanceof MultiValueInput) {
                    MultiValueInputControl input = new MultiValueInputControl(activity, null, (MultiValueInput) content);

                    LayoutParams lp;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        lp = new LayoutParams(controlLp);
                    } else {
                        lp = new LayoutParams(controlLp.width, controlLp.height);
                    }
                    lp.width = LayoutParams.MATCH_PARENT;
                    lp.height = LayoutParams.WRAP_CONTENT;

                    input.setLayoutParams(lp);
                    input.setTag(content.getTag());
                    final MultiValueInputControl mvicReference = input;
                    final String tag = content.getTag();
                    // Store the result of the content for processing later
                    input.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            HashMap<String, Object> result = new HashMap<String, Object>();
                            mvicReference.onReplyDataRequired(result);
                            stashMultiChoiceInputData(page.getTag(), tag, result);
                        }
                    });

                    contentLayout.addView(input);
                    inputs.add(input);
                } else if (content instanceof MultiValueLongInput) {
                    MultiValueLongInputControl input = MultiValueLongInputControl.inflate(activity, contentLayout, (MultiValueLongInput) content);
                    LayoutParams lp;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        lp = new LayoutParams(controlLp);
                    } else {
                        lp = new LayoutParams(controlLp.width, controlLp.height);
                    }
                    lp.width = LayoutParams.MATCH_PARENT;
                    lp.height = LayoutParams.WRAP_CONTENT;

                    input.setLayoutParams(lp);
                    final MultiValueLongInputControl mviclReference = input;
                    final String tag = content.getTag();
                    input.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            HashMap<String, Object> result = new HashMap<String, Object>();
                            mviclReference.onReplyDataRequired(result);
                            stashMultiChoiceLongInputData(page.getTag(), tag, result);
                        }
                    });
                    input.setTag(content.getTag());
                    contentLayout.addView(input);
                    inputs.add(input);
                } else if (content instanceof NPSInput) {
                    NPSlider slider = (NPSlider) getLayoutInflater(null).inflate(R.layout.cio__npslider, contentLayout, false);
                    slider.setModel((NPSInput) content);
                    final NPSlider sliderReference = slider;
                    final String tag = content.getTag();
                    slider.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            HashMap<String, Object> result = new HashMap<String, Object>();
                            sliderReference.onReplyDataRequired(result);
                            stashNPSInputData(page.getTag(), tag, result);
                        }
                    });
                    slider.setTag(content.getTag());
                    contentLayout.addView(slider);
                    inputs.add(slider);
                }
            }
        }
        sendPageImpressionEvent(page.getTag());
        root.requestFocus();
    }

    @Override
    public void onClick(View v) {
        if (v instanceof ConversationControl) {
            // Ok, lets do this....

            // When a control is clicked, a navigation event or action event occurs. We then send all the queued SwrveEvents which have been queued for this page
            commitUserInputsToEvents();

            if (v instanceof ConversationButton) {
                ConversationReply reply = new ConversationReply();
                ConversationButton convButton = (ConversationButton) v;
                ButtonControl model = convButton.getModel();
                if (((ConversationControl) v).getModel().hasActions()) {
                    ActionBehaviours behaviours = new ActionBehaviours(this.getActivity(), this.getActivity().getApplicationContext()) {
                    };
                    ControlActions actions = ((ConversationControl) v).getModel().getActions();
                    if (actions.isCall()) {
                        sendReply(model, reply);
                        sendCallActionEvent(page.getTag(), model);
                        behaviours.openDialer(actions.getCallUri(), this.getActivity());
                    } else if (actions.isVisit()) {
                        HashMap<String, String> visitUriDetails = (HashMap<String, String>) actions.getVisitDetails();
                        String urlStr = visitUriDetails.get(ControlActions.VISIT_URL_URI_KEY);
                        String referrer = visitUriDetails.get(ControlActions.VISIT_URL_REFERER_KEY);
                        String ext = visitUriDetails.get(ControlActions.VISIT_URL_EXTERNAL_KEY);
                        Uri uri = Uri.parse(urlStr);

                        if (Boolean.parseBoolean(ext) == true) {
                            sendReply(model, reply);
                            sendLinkActionEvent(page.getTag(), model);
                            behaviours.openIntentWebView(uri, this.getActivity(), referrer);
                        } else if (Boolean.parseBoolean(ext) == false) {
                            enforceValidations();
                            sendLinkActionEvent(page.getTag(), model);
                            behaviours.openPopupWebView(uri, this.getActivity(), referrer, "Back to Conversation");
                        } else {

                        }
                    } else if (actions.isDeepLink()) {
                        {
                            HashMap<String, String> visitUriDetails = (HashMap<String, String>) actions.getDeepLinkDetails();
                            String urlStr = visitUriDetails.get(ControlActions.DEEPLINK_URL_URI_KEY);
                            Uri uri = Uri.parse(urlStr);
                            enforceValidations();
                            sendDeepLinkActionEvent(page.getTag(), model);
                            behaviours.openDeepLink(uri, this.getActivity());
                        }
                    }
                } else {
                    // There are no actions associated with Button. Send a normal reply
                    sendReply(model, reply);
                }
            } else {
                // Unknown button type was clicked
            }
        }
    }

    /**
     * Go through each of the recorded interactions the user has with the page and queue them as events
     */
    private void commitUserInputsToEvents() {
        Log.i(LOG_TAG, "Commiting all stashed events");
        ArrayList<UserInputResult> userInputEvents = new ArrayList<>();
        for (String k : userInteractionData.keySet()) {
            UserInputResult r = userInteractionData.get(k);
            userInputEvents.add(r);
        }
        controller.conversationEventsCommitedByUser(swrveConversation, userInputEvents);
    }

    /**
     * Kick off sending reply. The input tree will be traversed and responses gathered. If additional data needs to be included, include in the reply before passing in.
     *
     * @param control
     * @param reply
     */
    private void sendReply(ControlBase control, ConversationReply reply) {

        reply.setControl(control.getTag());

        // For all the inputs , get their data
        for (ConversationInput inputView : inputs) {
            inputView.onReplyDataRequired(reply.getData());
        }

        ConversationPage nextPage = swrveConversation.getPageForControl(control);

        enforceValidations();

        if (nextPage != null) {
            if (isOkToProceed()) {
                sendTransitionPageEvent(page.getTag(), control.getTarget(), control.getTag());
                openConversationOnPage(nextPage);
            }else{
                Log.i(LOG_TAG, "User tried to go to the next piece of the conversation but it is not ok to proceed");
            }
        }
        else if (control.hasActions()) {
            if (isOkToProceed()) {
                Log.i(LOG_TAG, "User has selected an Action. They are now finished the conversation");
                sendDoneNavigationEvent(page.getTag(), control.getTag());
                getActivity().finish();
            }else{
                Log.i(LOG_TAG, "User tried to leave the conversation via an action but it is not ok to proceed");
            }
        } else {
            Log.e(LOG_TAG, "No more pages in this conversation");
            if (isOkToProceed()) {
                sendDoneNavigationEvent(page.getTag(), control.getTag()); // No exception. We just couldn't find a page attached to the control.
                getActivity().finish();
            }else{
                Log.i(LOG_TAG, "User tried to go to the next piece of the conversation but it is not ok to proceed");
            }
        }
    }

    public void onBackPressed() {
        sendCancelNavigationEvent(page.getTag());
        commitUserInputsToEvents();
    }

    private void enforceValidations() {
        ArrayList<String> validationErrors = new ArrayList<String>();

        // First, validate
        for (ConversationInput inputView : inputs) {
            String answer = inputView.validate();
            if (answer != null) {
                validationErrors.add(answer);
            }
        }

        if (validationErrors.size() > 0) {
            userInputValid = false;
            validationDialog = ValidationDialog.create("Please fill out all of the items on this page before continuing");
            validationDialog.show(getFragmentManager(), "validation_dialog");
            return;
        } else {
            userInputValid = true;
            return;
        }
    }

    private boolean isOkToProceed() {
        return userInputValid == true;
    }

    // Events
    private void sendPageImpressionEvent(String pageTag) {
        if (controller != null) {
            controller.conversationPageWasViewedByUser(swrveConversation, pageTag);
        }
    }

    private void sendStartNavigationEvent(String startPageTag) {
        if (controller != null) {
            controller.conversationWasStartedByUser(swrveConversation, startPageTag);
        }
    }

    private void sendDoneNavigationEvent(String endPageTag, String endControlTag) {
        if (controller != null) {
            controller.conversationWasFinishedByUser(swrveConversation, endPageTag, endControlTag);
        }
    }

    private void sendCancelNavigationEvent(String currentPageTag) {
        if (controller != null) {
            controller.conversationWasCancelledByUser(swrveConversation, currentPageTag);
        }
    }

    private void sendErrorNavigationEvent(String currentPageTag, Exception e) {
        if (controller != null) {
            controller.conversationEncounteredError(swrveConversation, currentPageTag, e);
        }
    }

    private void sendTransitionPageEvent(String currentPageTag, String targetPageTag, String controlTag) {
        if (controller != null) {
            controller.conversationTransitionedToOtherPage(swrveConversation, currentPageTag, targetPageTag, controlTag);
        }
    }

    private void sendLinkActionEvent(String currentPageTag, ConversationAtom control) {
        if (controller != null) {
            controller.conversationLinkActionCalledByUser(swrveConversation, currentPageTag, control.getTag());
        }
    }

    private void sendDeepLinkActionEvent(String currentPageTag, ConversationAtom control) {
        if (controller != null) {
            // TODO: implement this guy.
            // controller.conversationDeepLinkActionCalledByUser(swrveConversation, currentPageTag, control.getTag());
        }
    }


    private void sendCallActionEvent(String currentPageTag, ConversationAtom control) {
        if (controller != null) {
            controller.conversationCallActionCalledByUser(swrveConversation, currentPageTag, control.getTag());
        }
    }

    // For each of the content portions we store data about them which is then committed at a later point
    private void stashVideoViewed(String pageTag, String fragmentTag, HtmlVideoView v) {
        // TODO: Is there any data we can record about clicked video views?
        String key = pageTag + "-" + fragmentTag;
        String type = UserInputResult.TYPE_VIDEO_PLAY;
        UserInputResult result = new UserInputResult();
        result.type = type;
        result.conversationId = Integer.toString(swrveConversation.getId());
        result.fragmentTag = fragmentTag;
        result.pageTag = pageTag;
        result.result = "";
        userInteractionData.put(key, result);
    }

    private void stashMultiChoiceInputData(String pageTag, String fragmentTag, HashMap<String, Object> data) {
        String key = pageTag + "-" + fragmentTag;
        String type = UserInputResult.TYPE_SINGLE_CHOICE;
        for (String k : data.keySet()) {
            UserInputResult result = new UserInputResult();
            result.type = type;
            result.conversationId = Integer.toString(swrveConversation.getId());
            result.fragmentTag = fragmentTag;
            result.pageTag = pageTag;
            result.result = data.get(k);
            userInteractionData.put(key, result);
        }
    }

    private void stashMultiChoiceLongInputData(String pageTag, String fragmentTag, HashMap<String, Object> data) {
        String key = pageTag + "-" + fragmentTag;
        String type = UserInputResult.TYPE_MULTI_CHOICE;
        for (String k : data.keySet()) {
            ChoiceInputResponse userChoice = (ChoiceInputResponse) data.get(k);
            UserInputResult result = new UserInputResult();
            result.type = type;
            result.conversationId = Integer.toString(swrveConversation.getId());
            result.fragmentTag = fragmentTag;
            result.pageTag = pageTag;
            result.result = data.get(k);
            String userInteractionKey = key + "-" + userChoice.getQuestionID(); // Important to note, using fragment and page is not enough to store this input. It needs a unique identifier such as the question ID or something specific since it goes 1 level down further than other inputs
            userInteractionData.put(userInteractionKey, result);
        }
    }

    private void stashEditTextControlInputData(String pageTag, String fragmentTag, HashMap<String, Object> data) {
        String key = pageTag + "-" + fragmentTag;
        String type = UserInputResult.TYPE_TEXT;
        for (String k : data.keySet()) {
            UserInputResult result = new UserInputResult();
            result.type = type;
            result.conversationId = Integer.toString(swrveConversation.getId());
            result.fragmentTag = fragmentTag;
            result.pageTag = pageTag;
            result.result = data.get(k);
            userInteractionData.put(key, result);
        }
    }

    private void stashNPSInputData(String pageTag, String fragmentTag, HashMap<String, Object> data) {
        String key = pageTag + "-" + fragmentTag;
        String type = UserInputResult.TYPE_NPS;
        for (String k : data.keySet()) {
            UserInputResult result = new UserInputResult();
            result.type = type;
            result.conversationId = Integer.toString(swrveConversation.getId());
            result.fragmentTag = fragmentTag;
            result.pageTag = pageTag;
            result.result = data.get(k);
            userInteractionData.put(key, result);
        }
    }

    class ConversationPageException extends Exception
    {
        public ConversationPageException(String message)
        {
            super(message);
        }
    }
}