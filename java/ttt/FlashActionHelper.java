// TeleTeachingTool - Presentation Recording With Automated Indexing
//
// Copyright (C) 2003-2008 Peter Ziewer - Technische Universität München
// 
//    This file is part of TeleTeachingTool.
//
//    TeleTeachingTool is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    TeleTeachingTool is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with TeleTeachingTool.  If not, see <http://www.gnu.org/licenses/>.

/*
 * Created on 09.05.2007
 *
 * Author: Peter Ziewer, TU Munich, Germany - ziewer@in.tum.de
 * based on diploma thesis of Eric Willy Tiabou
 */
package ttt;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;

import javax.imageio.ImageIO;

import com.anotherbigidea.flash.SWFConstants;
import com.anotherbigidea.flash.movie.Actions;
import com.anotherbigidea.flash.movie.Button;
import com.anotherbigidea.flash.movie.EditField;
import com.anotherbigidea.flash.movie.Font;
import com.anotherbigidea.flash.movie.FontDefinition;
import com.anotherbigidea.flash.movie.FontLoader;
import com.anotherbigidea.flash.movie.Frame;
import com.anotherbigidea.flash.movie.ImageUtil;
import com.anotherbigidea.flash.movie.MovieClip;
import com.anotherbigidea.flash.movie.Shape;
import com.anotherbigidea.flash.movie.Text;
import com.anotherbigidea.flash.movie.Transform;
import com.anotherbigidea.flash.movie.Font.NoGlyphException;
import com.anotherbigidea.flash.structs.AlphaColor;
import com.anotherbigidea.flash.structs.AlphaTransform;

public class FlashActionHelper {

    public Recording recording;

    public MovieClip movieClip = new MovieClip(); // movieclip for slides
    Vector bufMovieClip = new Vector();// buffer for movieclip

    int thumbnailWidth, thumbnailHeight;

    protected FlashActionHelper() {

    };

    // ////////////////////////////////////////////////////////////////
    // Control elements
    // ////////////////////////////////////////////////////////////////
    Button createButtonPlay() throws IOException {
        Shape shapePlay = loadImage("resources/FlashPlay.png");

        Button buttonPlay = new Button(false);
        buttonPlay.addLayer(shapePlay, new Transform(), new AlphaTransform(), 0, true, true, false, true);
        Actions action = buttonPlay.addActions(SWFConstants.BUTTON2_OVERUP2OVERDOWN, 5);
        // if (isStop){}
        action.push("isStop");
        action.getVariable();
        action.not();
        action.ifJump("label0");

        // setOffsetByPlay(action);
        isStopFalse_TimeClipPlay(action);
        stopOrPlayCurrentMovieClip(action, "play");
        updateTimelabel(action);

        action.jumpLabel("label0");
        action.end();

        return buttonPlay;
    }

    Button createButtonStop() throws IOException {
        Shape shapeStop = loadImage("resources/FlashPause.png");

        Button buttonStop = new Button(false);
        buttonStop.addLayer(shapeStop, new Transform(), new AlphaTransform(), 0, true, true, false, true);
        Actions action = buttonStop.addActions(SWFConstants.BUTTON2_OVERUP2OVERDOWN, 5);
        isStopTrue_TimeClipStop(action);
        // modified by ziewer - 29.05.2007
        // ORIGINAL: stopCurrentMovieClip(action);
        stopOrPlayCurrentMovieClip(action, "stop");
        action.push("isStop");
        action.push(true);
        action.setVariable();
        // end of modification by ziewer - 29.05.2007

        setStopTime(action);
        action.end();

        return buttonStop;
    }

    Button createButtonNextSlide() throws IOException {
        Shape shapeNext = loadImage("resources/FlashNext.png");

        Button buttonNextSlide = new Button(false);
        buttonNextSlide.addLayer(shapeNext, new Transform(), new AlphaTransform(), 0, true, true, false, true);
        Actions action = buttonNextSlide.addActions(SWFConstants.BUTTON2_OVERUP2OVERDOWN, 5);
        // if(_root.mcNumber < bufMovieClip.size() -1){
        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        action.getMember();
        action.push(bufMovieClip.size() - 1);
        action.typedEquals();
        action.not();
        action.not();
        action.ifJump("label0");

        // setOffsetByNextSlide(action);
        isStopFalse_TimeClipPlay(action);
        stopCurrentClipInFrameBeforeLast(action);
        setCurrentClipVisibilty(action, false);
        gotoFirstFrameOfNextSlideAndPlay(action);
        setNextClipVisibilty(action, true);
        updateTimelabel(action);

        scrollThumbnailUp("thumbnailsClip", action, recording.index.size(), thumbnailHeight);
        action.jumpLabel("label0");

        action.end();

        return buttonNextSlide;
    }

    Button createButtonPreviousSlide() throws IOException {
        Shape shapePrev = loadImage("resources/FlashPrev.png");

        Button buttonPreviousSlide = new Button(false);
        buttonPreviousSlide.addLayer(shapePrev, new Transform(), new AlphaTransform(), 0, true, true, false, true);
        Actions action = buttonPreviousSlide.addActions(SWFConstants.BUTTON2_OVERUP2OVERDOWN, 5);
        // if(_root.mcNumber > 0){
        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        action.getMember();
        action.push(0.0);
        action.typedEquals();
        action.not();
        action.not();
        action.ifJump("label0");

        setOffsetByPreviousSlide(action);
        isStopFalse_TimeClipPlay(action);
        stopCurrentClipInFrameBeforeLast(action);
        setCurrentClipVisibilty(action, false);
        gotoFirstFrameOfPreviousSlideAndPlay(action);
        setPreviousClipVisibilty(action, true);

        scrollThumbnailDown("thumbnailsClip", action, recording.index.size(), thumbnailHeight);

        action.jumpLabel("label0");
        action.end();

        return buttonPreviousSlide;
    }

    Button createButtonBackward() throws IOException {
        Shape shapeNext = loadImage("resources/FlashFastBackward.png");

        Button button = new Button(false);
        button.addLayer(shapeNext, new Transform(), new AlphaTransform(), 0, true, true, false, true);
        Actions action = button.addActions(SWFConstants.BUTTON2_OVERUP2OVERDOWN, 5);

        gotoCurrentFramePlusSeconds(action, -20);
        updateTimelabel(action);

        action.end();

        return button;
    }

    Button createButtonForward() throws IOException {
        Shape shapeNext = loadImage("resources/FlashFastForward.png");

        Button button = new Button(false);
        button.addLayer(shapeNext, new Transform(), new AlphaTransform(), 0, true, true, false, true);
        Actions action = button.addActions(SWFConstants.BUTTON2_OVERUP2OVERDOWN, 5);

        gotoCurrentFramePlusSeconds(action, 20);
        updateTimelabel(action);

        action.end();

        return button;
    }

    Button createButtonScrollThumbnailDown() throws IOException {
        Shape shapeScrollThumbnailDown = loadImage("resources/FlashDownWide.png");

        Button buttonScrollThumbnailDown = new Button(false);
        buttonScrollThumbnailDown.addLayer(shapeScrollThumbnailDown, new Transform(), new AlphaTransform(), 0, true,
                true, false, true);
        Actions action = buttonScrollThumbnailDown.addActions(SWFConstants.BUTTON2_OVERUP2OVERDOWN, 5);

        // u = bufMovieClip.size() -1;
        // if (_root["thumbnailsClip"+u]._y < framebufferHeight - 20){}
        action.lookupTable(new String[] { "u", "_root", "thumbnailsClip", "_y" });
        action.lookup(0);
        action.push(bufMovieClip.size() - 1);
        action.setVariable();
        action.push(recording.prefs.framebufferHeight - 20);
        action.lookup(1);
        action.getVariable();
        action.lookup(2);
        action.lookup(0);
        action.getVariable();
        action.typedAdd();
        action.getMember();
        action.lookup(3);
        action.getMember();

        action.typedLessThan();
        action.not();
        action.ifJump("label0");

        scrollThumbnailUp("thumbnailsClip", action, recording.index.size(), thumbnailHeight);
        scrollMarkerUp("clipMarker", action, thumbnailHeight);

        action.jumpLabel("label0");

        action.end();

        return buttonScrollThumbnailDown;
    }

    Button createButtonScrollThumbnailUp() throws IOException {
        Shape shapeScrollThumbnailUp = loadImage("resources/FlashUpWide.png");

        Button buttonScrollThumbnailUp = new Button(false);
        buttonScrollThumbnailUp.addLayer(shapeScrollThumbnailUp, new Transform(), new AlphaTransform(), 0, true, true,
                false, true);
        Actions action = buttonScrollThumbnailUp.addActions(SWFConstants.BUTTON2_OVERUP2OVERDOWN, 5);

        // if (_root.thumbnailsClip0._y <= 0){}
        action.push("_root");
        action.getVariable();
        action.push("thumbnailsClip0");
        action.getMember();
        action.push("_y");
        action.getMember();
        action.push(0);
        action.typedLessThan();
        action.not();
        action.ifJump("label0");

        scrollThumbnailDown("thumbnailsClip", action, recording.index.size(), thumbnailHeight);
        scrollMarkerDown("clipMarker", action, thumbnailHeight);

        action.jumpLabel("label0");

        action.end();

        return buttonScrollThumbnailUp;
    }

    // ziewer - modified to load resources from jar
    Shape insertJPEGButton(String fileName) throws IOException {
        URL url = this.getClass().getResource(fileName);

        // open the JPEG
        InputStream jpegIn = url.openStream();

        // create a shape that uses the image as a fill
        // (images cannot be placed directly. They can only be used as shape fills)
        int[] size = new int[2];
        com.anotherbigidea.flash.movie.Shape image = ImageUtil.shapeForImage(jpegIn, size);

        int width = size[0];
        int height = size[1];
        jpegIn.close();

        image.defineLineStyle(1, null); // default color is black
        image.setLineStyle(1);
        image.line(width, 0);
        image.line(width, height);
        image.line(0, height);
        image.line(0, 0);

        return image;
    }

    // ziewer - modified to load resources from jar
    Shape loadImage(String fileName) throws IOException {
        URL url = this.getClass().getResource(fileName);
        Image img = ImageIO.read(url);

        // create a shape that uses the image as a fill
        // (images cannot be placed directly. They can only be used as shape fills)
        com.anotherbigidea.flash.movie.Image losslessImage = ImageUtil.createLosslessImage(img,
                SWFConstants.BITMAP_FORMAT_32_BIT, false);
        com.anotherbigidea.flash.movie.Shape image = ImageUtil.shapeForImage(losslessImage, img.getWidth(null), img
                .getHeight(null));

        return image;
    }

    // ////////////////////////////////////////////////////////////////
    // Help methods to control movieclips
    // ////////////////////////////////////////////////////////////////
    static void stopCurrentMovieClip(Actions action) throws IOException {
        // _root["mclip"+_root.mcNumber].gotoAndStop(2);
        // action.lookupTable(new String[] { "_root", "mclip", "mcNumber", "nextFrameAndStop" });
        action.lookupTable(new String[] { "_root", "mclip", "mcNumber", "gotoAndStop" });
        action.push(2);
        action.push(1);
        action.lookup(0);
        action.getVariable();
        action.lookup(1);
        action.lookup(0);
        action.getVariable();
        action.lookup(2);
        action.getMember();
        action.typedAdd();
        action.getMember();
        action.lookup(3);
        action.callMethod();
        action.pop();
    }

    static void gotoFirstFrameOfPreviousSlideAndPlay(Actions action) throws IOException {
        // _root["mclip"+(_root.mcNumber + 1 )].gotoAndPlay(1);
        action.lookupTable(new String[] { "_root", "mclip", "mcNumber", "gotoAndPlay" });
        action.push(1);
        action.push(1);
        action.lookup(0); // push "_root"
        action.getVariable();
        action.lookup(1); // push "mclip"
        action.lookup(0); // push "_root"
        action.getVariable();
        action.lookup(2); // push "mcNumber"
        action.getMember();
        action.push(1);
        action.substract();
        action.typedAdd();
        action.getMember();
        action.lookup(3);
        action.callMethod();
        action.pop();
    }

    static void gotoFirstFrameOfNextSlideAndPlay(Actions action) throws IOException {
        // _root["mclip"+(_root.mcNumber + 1 )].gotoAndPlay(1);
        action.lookupTable(new String[] { "_root", "mclip", "mcNumber", "gotoAndPlay" });
        action.push(1);
        action.push(1);
        action.lookup(0);
        action.getVariable();
        action.lookup(1);
        action.lookup(0);
        action.getVariable();
        action.lookup(2);
        action.getMember();
        action.push(1);
        action.typedAdd();
        action.typedAdd();
        action.getMember();
        action.lookup(3);
        action.callMethod();
        action.pop();
    }

    static void gotoFirstFrameOfPreviousSlideAndPlay2(Actions action) throws IOException {
        // _root.mcNumber = _root.mcNumber - 1;
        // _root.mcNumber
        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        // _root.mcNumber - 1;
        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        action.getMember();
        action.push(1);
        action.substract();
        // =
        action.setMember();

        // _root["mclip"+_root.mcNumber].gotoAndPlay(1);
        action.push(1);
        action.push(1);
        action.push("_root");
        action.getVariable();
        action.push("mclip");
        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        action.getMember();
        action.typedAdd();
        action.getMember();
        action.push("gotoAndPlay");
        action.callMethod();
        action.pop();
    }

    static void gotoFirstFrameOfNextSlideAndPlay2(Actions action) throws IOException {
        // _root.mcNumber = _root.mcNumber + 1;
        // _root.mcNumber
        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        // _root.mcNumber + 1;
        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        action.getMember();
        action.push(1);
        action.typedAdd();
        // =
        action.setMember();

        // _root["mclip"+_root.mcNumber].gotoAndPlay(1);
        action.push(1);
        action.push(1);
        action.push("_root");
        action.getVariable();
        action.push("mclip");
        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        action.getMember();
        action.typedAdd();
        action.getMember();
        action.push("gotoAndPlay");
        action.callMethod();
        action.pop();
    }

    static void getCurrentFrameOfClip(Actions action) throws IOException {
        action.push("_root");
        action.getVariable();
        action.push("mclip");
        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        action.getMember();
        action.typedAdd();
        action.getMember();
        action.push("_currentframe");
        action.getMember();
    }

    static void getTotalFramesOfClip(Actions action) throws IOException {
        action.push("_root");
        action.getVariable();
        action.push("mclip");
        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        action.getMember();
        action.typedAdd();
        action.getMember();
        action.push("_totalframes");
        action.getMember();
    }

    static void gotoCurrentFramePlusSeconds(Actions action, int seconds) throws IOException {
        action.lookupTable(new String[] { "frc", "_root", "mclip", "mcNumber", "_currentframe", "gotoAndPlay" });

        // TODO: does not delete annotations if going backwards

        // currentframe +framerate*30
        getCurrentFrameOfClip(action);
        action.push(FlashContext.frameRate * seconds);
        action.add();

        // if ( value < 0 ) value = 0;
        action.duplicate();
        action.push(0);
        action.lessThan();
        action.not();
        action.ifJump("label_0");
        // value < 0
        action.pop();
        // TODO: goto previous index
        action.push(1); // goto first frame
        action.jumpLabel("label_0");

        // if ( value > maxFrames ) value = 0;
        action.duplicate();
        getTotalFramesOfClip(action);
        action.lessThan();
        action.ifJump("label_1");
        // value >= maxFrames
        action.pop();
        getTotalFramesOfClip(action); // goto last frame (which will result in next index)
        action.jumpLabel("label_1");

        // _root["mc"+_root.mcNumber].gotoAndPlay(frameNr);
        // frameNr is top of stack
        action.push(1);
        action.lookup(1);
        action.getVariable();
        action.lookup(2);
        action.lookup(1);
        action.getVariable();
        action.lookup(3);
        action.getMember();
        action.typedAdd();
        action.getMember();
        action.lookup(5);
        action.callMethod();
        action.pop();
    }

    static void setNextClipVisibilty(Actions action, boolean visible) throws IOException {
        // _root["mc"+(_root.mcNumber + 1)]._visible = true;
        action.lookupTable(new String[] { "_root", "mclip", "mcNumber", "_visible" });
        action.lookup(0);
        action.getVariable();
        action.lookup(1);
        action.lookup(0);
        action.getVariable();
        action.lookup(2);
        action.getMember();
        action.push(1);
        action.typedAdd();
        action.typedAdd();
        action.getMember();
        action.lookup(3);
        action.push(visible);
        action.setMember();
    }

    static void setCurrentClipVisibilty(Actions action, boolean visible) throws IOException {
        // _root["mc"+(_root.mcNumber)]._visible = true;
        action.lookupTable(new String[] { "_root", "mclip", "mcNumber", "_visible" });
        action.lookup(0);
        action.getVariable();
        action.lookup(1);
        action.lookup(0);
        action.getVariable();
        action.lookup(2);
        action.getMember();
        action.typedAdd();
        action.getMember();
        action.lookup(3);
        action.push(visible);
        action.setMember();
    }

    static void setPreviousClipVisibilty(Actions action, boolean visible) throws IOException {
        // _root["mclip"+(_root.mcNumber - 1)]._visible = true;
        action.lookupTable(new String[] { "_root", "mclip", "mcNumber", "_visible" });
        action.lookup(0);
        action.getVariable();
        action.lookup(1);
        action.lookup(0);
        action.getVariable();
        action.lookup(2);
        action.getMember();
        action.push(1);
        action.substract();
        action.typedAdd();
        action.getMember();
        action.lookup(3);
        action.push(visible);
        action.setMember();
    }

    static void stopCurrentMovieClipInFrameNbr(Actions action, int frameNumber) throws IOException {
        // _root["mc"+_root.mcNumber].gotoAndStop(frameNumber);
        action.lookupTable(new String[] { "_root", "mclip", "mcNumber", "gotoAndStop" });
        action.push(frameNumber);
        action.push(1);
        action.lookup(0);
        action.getVariable();
        action.lookup(1);
        action.lookup(0);
        action.getVariable();
        action.lookup(2);
        action.getMember();
        action.typedAdd();
        action.getMember();
        action.lookup(3);
        action.callMethod();
        action.pop();
    }

    static void stopOrPlayCurrentMovieClip(Actions action, String stopOrplay) throws IOException {
        // _root["mc"+ mcNumber].stop();
        action.push(0.0);
        action.push("_root");
        action.getVariable();
        action.push("mclip");
        action.push("mcNumber");
        action.getVariable();
        action.typedAdd();
        action.getMember();
        action.push(stopOrplay);
        action.callMethod();
        action.pop();
    }

    static void setMovieClipVisibility(String mc, boolean trueOrfalse, Actions action) throws IOException {
        // _root.mc._visible = true oder false;
        action.push("_root");
        action.getVariable();
        action.push(mc);
        action.getMember();
        action.push("_visible");
        action.push(trueOrfalse);
        action.setMember();
    }

    static void movieClipGotoFramePlay(int frame, String mc, Actions action) throws IOException {
        action.push(frame);
        action.push(1);
        action.push("_root");
        action.getVariable();
        action.push(mc);
        action.getMember();
        action.push("gotoAndPlay");
        action.callMethod();
        action.pop();
    }

    static void movieClipGotoFrameStop(int frame, String mc, Actions action) throws IOException {
        action.push(frame);
        action.push(1);
        action.push("_root");
        action.getVariable();
        action.push(mc);
        action.getMember();
        action.push("gotoAndStop");
        action.callMethod();
        action.pop();
    }

    static void stopCurrentClipInFrameBeforeLast(Actions actions) throws IOException {
        // frc = _root["mclip"+_root.mcNumber]._totalframes;
        actions.lookupTable(new String[] { "frc", "_root", "mclip", "mcNumber", "_totalframes", "gotoAndStop" });
        actions.lookup(0);
        actions.lookup(1);
        actions.getVariable();
        actions.lookup(2);
        actions.lookup(1);
        actions.getVariable();
        actions.lookup(3);
        actions.getMember();
        actions.typedAdd();
        actions.getMember();
        actions.lookup(4);
        actions.getMember();
        actions.setVariable();

        // _root["mc"+_root.mcNumber].gotoAndStop(frc -1);
        actions.lookup(0);
        actions.getVariable();
        actions.push(1);

        actions.substract();
        actions.push(1);

        actions.lookup(1);
        actions.getVariable();
        actions.lookup(2);
        actions.lookup(1);
        actions.getVariable();
        actions.lookup(3);
        actions.getMember();
        actions.typedAdd();
        actions.getMember();
        actions.lookup(5);
        actions.callMethod();
        actions.pop();
    }

    // //////////////////////////////////////////////////////////////////////////
    // Methods to set the offset during the calculation of the current playback
    // time after jumping to another slide
    // /////////////////////////////////////////////////////////////////////////

    static void setOffsetByPreviousSlide(Actions action) throws IOException {
        // _root.offset = _root["mclip"+(i-1)].timeStamp - getTimer();
        action.lookupTable(new String[] { "_root", "offset", "mclip", "mcNumber", "indexTimeStamp" });
        action.lookup(0);
        action.getVariable();
        action.lookup(1);

        action.lookup(0);
        action.getVariable();

        action.lookup(2);

        action.lookup(0);
        action.getVariable();
        action.lookup(3);
        action.getMember();

        action.push(1);
        action.substract();

        action.typedAdd();
        action.getMember();

        action.lookup(4);
        action.getMember();

        action.getTime();
        action.substract();
        action.setMember();
    }

    static void getTimestampOfClip(Actions action) throws IOException {
        // _root["mclip"+mcNumber].indexTimeStamp;
        action.push("_root");
        action.getVariable();
        action.push("mclip");

        action.push("_root");
        action.getVariable();
        action.push("mcNumber");
        action.getMember();

        action.typedAdd();
        action.getMember();
        action.push("indexTimeStamp");
        action.getMember();
    }

    static void updateTimelabel(Actions action) throws IOException {
        // root.offset =
        action.push("_root");
        action.getVariable();
        action.push("offset");

        // timestamp of index
        getTimestampOfClip(action);

        // timestamp within index (frame*1000/framerate)
        getCurrentFrameOfClip(action);
        action.push(1000);
        action.multiply();
        action.push(FlashContext.frameRate);
        action.divide();

        action.add();

        // minus play time since beginning
        action.getTime();
        action.substract();

        // root.offset =
        action.setMember();
    }

    static void setOffsetByThumbnail(int i, Actions action) throws IOException {
        // j= i;
        // offset = _root["mclip"+j].timeStamp - getTimer();
        action.lookupTable(new String[] { "j", "_root", "offset", "mclip", "indexTimeStamp" });
        action.lookup(0);
        action.push(i);
        action.setVariable();
        action.lookup(1);
        action.getVariable();
        action.lookup(2);
        action.lookup(1);
        action.getVariable();
        action.lookup(3);
        action.lookup(0);
        action.getVariable();
        action.typedAdd();
        action.getMember();
        action.lookup(4);
        action.getMember();
        action.getTime();
        action.substract();
        action.setMember();
    }

    // //////////////////////////////////////////////////////////////////////////
    // Methods to control the timer clip
    // /////////////////////////////////////////////////////////////////////////
    static void setStopTime(Actions action) throws IOException {
        // stopTime = _root.timeClip.currentTime;
        action.push("stopTime");
        action.push("_root");
        action.getVariable();
        action.push("timeClip");
        action.getMember();
        action.push("currentTime");
        action.getMember();
        action.setVariable();
    }

    static void isStopFalse_TimeClipPlay(Actions action) throws IOException {
        // _root.isStop = false;
        // _root.timeClip.play();
        action.lookupTable(new String[] { "_root", "isStop", "timeClip", "play" });
        action.lookup(0);
        action.getVariable();
        action.lookup(1);
        action.push(false);
        action.setMember();
        action.push(0.0);
        action.lookup(0);
        action.getVariable();
        action.lookup(2);
        action.getMember();
        action.lookup(3);
        action.callMethod();
        action.pop();
    }

    static void isStopTrue_TimeClipStop(Actions action) throws IOException {
        action.push("isStop");
        action.push(true);
        action.setVariable();

        action.push(0.0);
        action.push("_root");
        action.getVariable();
        action.push("timeClip");
        action.getMember();
        action.push("stop");
        action.callMethod();
        action.pop();
    }

    static void stopTimeClip(Actions action) throws IOException {
        action.push(0.0);
        action.push("_root");
        action.getVariable();
        action.push("timeClip");
        action.getMember();
        action.push("stop");
        action.callMethod();
        action.pop();
    }

    static FontDefinition fontdef;
    static {
        try {
            // load the font from an extern SWF-File (in this case VerdanaFont.swf )
            // NOTE: new Constants() is a little hack but an instance of some class from ttt package is required
            URL urlVerdanaFont = new Constants().getClass().getResource("resources/VerdanaFont.swf");
            fontdef = FontLoader.loadFont(urlVerdanaFont.openStream());
        } catch (Exception e) {
            System.out.println("CANNOT LOAD FONT FOR FLASH CONVERTER: " + e);
            e.printStackTrace();
        }
    }

    // //////////////////////////////////////////////////////////////////////////
    // Timer
    // The timer movieclip has 2 frames. The current playback time are compute in
    // the second frame and displayed in the first frame
    // //////////////////////////////////////////////////////////////////////////

    MovieClip createPlaybackTimefield(ProtocolPreferences preferences, int flashVersion) throws IOException {
        Font font = new Font(fontdef);
        EditField field = new EditField("editField", null, font, 24, 0, 0, 76, 40);
        field.setTextColor(new AlphaColor(0, 0, 0, 255));
        field.setAlignment(SWFConstants.TEXTFIELD_ALIGN_CENTER);

        field.setProperties(false, !true, false, true, false, false, false, false);

        // movieclip for timer
        MovieClip timeClip = new MovieClip();

        Frame timeFrame = timeClip.appendFrame();
        // timeFrame.placeSymbol(field, preferences.framebufferWidth / 2 - 12, preferences.framebufferHeight + 17, 1);
        // add border
        timeFrame.placeSymbol(loadImage("resources/FlashTimeBorder.png"), 2, 2);
        // add time label
        timeFrame.placeSymbol(field, 0, 4);

        Frame timeFrame2 = timeClip.appendFrame();
        Actions acts = timeFrame2.actions(flashVersion);

        /*
         * function actualTime( ){ currentTime = getTimer() + _root.offset; sec = Math.floor(currentTime/ 1000 % 60);
         * min = Math.floor(currentTime / 60000); if (sec<10) { sec = "0"+sec; } if (min<10) { min = "0"+min; }
         * editField = min+":"+sec ; }
         */
        acts.lookupTable(new String[] { "currentTime", "_root", "offset", "sec", "Math", "floor", "min", "0",
                "editField", ":" });
        acts.startFunction("actualTime", new String[] {});
        acts.lookup(0);
        acts.getTime();
        acts.lookup(1);
        acts.getVariable();
        acts.lookup(2);
        acts.getMember();
        acts.typedAdd();
        acts.setVariable();
        acts.lookup(3);
        acts.lookup(0);
        acts.getVariable();
        acts.push(1000);
        acts.divide();
        acts.push(60);
        acts.modulo();

        acts.push(1);
        acts.lookup(4);
        acts.getVariable();
        acts.lookup(5);
        acts.callMethod();
        acts.setVariable();
        acts.lookup(6);
        acts.lookup(0);
        acts.getVariable();
        acts.push(60000);
        acts.divide();

        acts.push(1);
        acts.lookup(4);
        acts.getVariable();
        acts.lookup(5);
        acts.callMethod();
        acts.setVariable();
        acts.lookup(3);
        acts.getVariable();
        acts.push(10);

        acts.typedLessThan();
        acts.not();
        acts.ifJump("label0");
        acts.lookup(3);
        acts.lookup(7);
        acts.lookup(3);
        acts.getVariable();
        acts.typedAdd();
        acts.setVariable();
        acts.jumpLabel("label0");

        acts.lookup(6);
        acts.getVariable();
        acts.push(10);
        acts.typedLessThan();
        acts.not();
        acts.ifJump("label1");
        acts.lookup(6);
        acts.lookup(7);
        acts.lookup(6);
        acts.getVariable();
        acts.typedAdd();
        acts.setVariable();
        acts.jumpLabel("label1");

        acts.lookup(8);
        acts.lookup(6);
        acts.getVariable();
        acts.lookup(9);
        acts.typedAdd();
        acts.lookup(3);
        acts.getVariable();
        acts.typedAdd();
        acts.setVariable();

        acts.endBlock(); // }

        acts.gotoFrame(1); // actually frame 2
        acts.play();

        // actualTime() ;
        acts.push("0.0");
        acts.push("actualTime");
        acts.callFunction();
        acts.pop();

        acts.end();
        timeFrame2.setActions(acts);
        acts.done();

        return timeClip;
    }

    static void scrollThumbnailUp(String clip, Actions action, int indexSize, int thumbnailHeight) throws IOException {
        // _root.clip._y -= thumbnailHeight;
        for (int i = 0; i < indexSize; i++) {
            action.push("_root");
            action.getVariable();
            action.push(clip + i);
            action.getMember();
            action.push("_y");
            action.push("_root");
            action.getVariable();
            action.push(clip + i);
            action.getMember();
            action.push("_y");
            action.getMember();
            action.push(thumbnailHeight + 4);
            action.substract();
            action.setMember();
        }
    }

    static void scrollThumbnailDown(String clip, Actions action, int indexSize, int thumbnailHeight) throws IOException {
        // _root.clip._y -= thumbnailHeight;
        for (int i = 0; i < indexSize; i++) {
            action.push("_root");
            action.getVariable();
            action.push(clip + i);
            action.getMember();
            action.push("_y");
            action.push("_root");
            action.getVariable();
            action.push(clip + i);
            action.getMember();
            action.push("_y");
            action.getMember();
            action.push(thumbnailHeight + 4);
            action.typedAdd();
            action.setMember();
        }
    }

    static void scrollMarkerUp(String clip, Actions action, int thumbnailHeight) throws IOException {
        // _root.clip._y += thumbnailHeight;
        action.push("_root");
        action.getVariable();
        action.push(clip);
        action.getMember();
        action.push("_y");
        action.push("_root");
        action.getVariable();
        action.push(clip);
        action.getMember();
        action.push("_y");
        action.getMember();
        action.push(thumbnailHeight + 4);
        action.substract();
        action.setMember();
    }

    static void scrollMarkerDown(String clip, Actions action, int thumbnailHeight) throws IOException {
        // _root.clip._y -= thumbnailHeight;
        action.push("_root");
        action.getVariable();
        action.push(clip);
        action.getMember();
        action.push("_y");
        action.push("_root");
        action.getVariable();
        action.push(clip);
        action.getMember();
        action.push("_y");
        action.getMember();
        action.push(thumbnailHeight + 4);
        action.typedAdd();
        action.setMember();
    }

    // //////////////////////////////////////////////////////////////////////////
    // Help method to insert text. This method create a text symbol.
    // /////////////////////////////////////////////////////////////////////////
    public static Text createText(String textContent, double fontSize) throws IOException {
        // Create a text object with a default transform
        Text text = new Text(null);

        // The font references the Font Definition and pulls over only the
        // glyph definitions that are required for any text referencing the font
        Font font = new Font(fontdef);

        // Add a row of characters; specify the starting (x,y) within the text symbol
        try {
            text.row(font.chars(textContent, fontSize), new com.anotherbigidea.flash.structs.Color(0, 0, 0), 0.0, 0.0,
                    true, true);
        } catch (NoGlyphException e) {
            throw new IOException("Flash Font Error: NoGlyphException: " + e);
        }

        return text;
    }
}
