package com.wwj.download;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.demoplay.R;
import com.example.demoplay.bean.Music;
import com.wwj.download.util.Player;
import com.wwj.net.download.DownloadProgressListener;
import com.wwj.net.download.FileDownloader;

public class PlayActivity extends Activity {
	private static final int PROCESSING = 1;
	private static final int FAILURE = -1;

	private static final int CONDITION = 3;

	private EditText pathText; // url��ַ
	private TextView resultView;// ���ؽ���
	private Button downloadButton;// ��ʼ����
	private Button stopButton;// ��ͣ����
	private Button up;
	private Button down;
	private Button pause;
	private ProgressBar progressBar;
	private Button playBtn;
	private Player player;
	private SeekBar musicProgress;
	private SimpleAdapter adapter;
	private ListView listview;
	List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
	List<Music> musiclists = new ArrayList<Music>();
	private Handler handler = new UIHandler();

	private final class UIHandler extends Handler {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case PROCESSING: // ���½���
				progressBar.setProgress(msg.getData().getInt("size"));
				float num = (float) progressBar.getProgress()
						/ (float) progressBar.getMax();
				int result = (int) (num * 100); // �������
				resultView.setText(result + "%");
				if (progressBar.getProgress() == progressBar.getMax()) { // �������
					Toast.makeText(getApplicationContext(), R.string.success,
							Toast.LENGTH_LONG).show();
				}
				break;
			case FAILURE: // ����ʧ��
				Toast.makeText(getApplicationContext(), R.string.error,
						Toast.LENGTH_LONG).show();
				break;
			case CONDITION:
				pause.setText(msg.obj.toString());
				break;
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		initView();
		musicList();
		
	}

	private void initView() {
		pathText = (EditText) findViewById(R.id.path);

		resultView = (TextView) findViewById(R.id.resultView);
		downloadButton = (Button) findViewById(R.id.downloadbutton);
		stopButton = (Button) findViewById(R.id.stopbutton);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
		ButtonClickListener listener = new ButtonClickListener();
		downloadButton.setOnClickListener(listener);
		stopButton.setOnClickListener(listener);
		playBtn = (Button) findViewById(R.id.btn_online_play);// ����
		playBtn.setOnClickListener(listener);
		musicProgress = (SeekBar) findViewById(R.id.music_progress);
		player = new Player(musicProgress);
		musicProgress.setOnSeekBarChangeListener(new SeekBarChangeEvent());

		pause = (Button) findViewById(R.id.btn_puse_play);
		up = (Button) findViewById(R.id.btn_up_play);
		down = (Button) findViewById(R.id.btn_down_play);

		pause.setOnClickListener(listener);
		up.setOnClickListener(listener);
		down.setOnClickListener(listener);
	}

	private final class ButtonClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.downloadbutton: // ��ʼ����
				downMusic();
				player.playUrl(pathText.getText().toString());

				downloadButton.setEnabled(false);
				stopButton.setEnabled(true);
				playBtn.setEnabled(false);
				break;
			case R.id.stopbutton: // ��ͣ����
				exit();
				Toast.makeText(getApplicationContext(),
						"Now thread is Stopping!!", Toast.LENGTH_LONG).show();
				downloadButton.setEnabled(true);
				stopButton.setEnabled(false);
				player.stop();
				playBtn.setEnabled(true);
				break;
			case R.id.btn_online_play:
				new Thread(new Runnable() {

					@Override
					public void run() {

						player.playUrl(pathText.getText().toString());

					}
				}).start();
				pause.setEnabled(true);
				playBtn.setEnabled(false);
				break;
			case R.id.btn_puse_play:// ��ͣ|| �ָ�
				String condition = null;
				if (player.isPlaying()) {
					player.pause();
					condition = "��ͣ";
				} else {
					player.play();
					condition = "�ָ�";
				}
				Message msg = handler.obtainMessage(CONDITION, condition);
				handler.sendMessage(msg);
				playBtn.setEnabled(false);
				break;
			case R.id.btn_down_play:// ��һ��
				player.next(musiclists);
				break;
			case R.id.btn_up_play:// ��һ��
				player.previous(musiclists);
				break;
			}
		}

		private void downMusic() {
			// http://abv.cn/music/�������.mp3�����Ի��������ļ����ص�����
			String path = pathText.getText().toString();
			String filename = path.substring(path.lastIndexOf('/') + 1);

			try {
				// URL���루������Ϊ�˽����Ľ���URL���룩
				filename = URLEncoder.encode(filename, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			path = path.substring(0, path.lastIndexOf("/") + 1) + filename;
			if (Environment.getExternalStorageState().equals(
					Environment.MEDIA_MOUNTED)) {
				// ����·��
				File savDir = Environment.getExternalStorageDirectory();
				download(path, savDir);
			} else {
				Toast.makeText(getApplicationContext(), R.string.sdcarderror,
						Toast.LENGTH_LONG).show();
			}

		}

		/*
		 * �����û��������¼�(���button, ������Ļ....)�������̸߳�����ģ�������̴߳��ڹ���״̬��
		 * ��ʱ�û������������¼����û����5���ڵõ�����ϵͳ�ͻᱨ��Ӧ������Ӧ������
		 * ���������߳��ﲻ��ִ��һ���ȽϺ�ʱ�Ĺ���������������߳��������޷������û��������¼���
		 * ���¡�Ӧ������Ӧ������ĳ��֡���ʱ�Ĺ���Ӧ�������߳���ִ�С�
		 */
		private DownloadTask task;

		private void exit() {
			if (task != null)
				task.exit();
		}

		private void download(String path, File savDir) {
			task = new DownloadTask(path, savDir);
			new Thread(task).start();
		}

		/**
		 * 
		 * UI�ؼ�������ػ�(����)�������̸߳�����ģ���������߳��и���UI�ؼ���ֵ�����º��ֵ�����ػ浽��Ļ��
		 * һ��Ҫ�����߳������UI�ؼ���ֵ��������������Ļ����ʾ���������������߳��и���UI�ؼ���ֵ
		 * 
		 */
		private final class DownloadTask implements Runnable {
			private String path;
			private File saveDir;
			private FileDownloader loader;

			public DownloadTask(String path, File saveDir) {
				this.path = path;
				this.saveDir = saveDir;
			}

			/**
			 * �˳�����
			 */
			public void exit() {
				if (loader != null)
					loader.exit();
			}

			DownloadProgressListener downloadProgressListener = new DownloadProgressListener() {
				@Override
				public void onDownloadSize(int size) {
					Message msg = new Message();
					msg.what = PROCESSING;
					msg.getData().putInt("size", size);
					handler.sendMessage(msg);
				}
			};

			public void run() {
				try {
					// ʵ����һ���ļ�������
					loader = new FileDownloader(getApplicationContext(), path,
							saveDir, 3);
					// ���ý��������ֵ
					progressBar.setMax(loader.getFileSize());
					loader.download(downloadProgressListener);
				} catch (Exception e) {
					e.printStackTrace();
					handler.sendMessage(handler.obtainMessage(FAILURE)); // ����һ������Ϣ����
				}
			}
		}
	}

	class SeekBarChangeEvent implements OnSeekBarChangeListener {
		int progress;

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// ԭ����(progress/seekBar.getMax())*player.mediaPlayer.getDuration()
			this.progress = progress * player.mediaPlayer.getDuration()
					/ seekBar.getMax();
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// seekTo()�Ĳ����������ӰƬʱ������֣���������seekBar.getMax()��Ե�����
			player.mediaPlayer.seekTo(progress);
		}

	}

	/* �����б� */
	public void musicList() {
		// ȡ��ָ��λ�õ��ļ�������ʾ�������б�
		String[] music = new String[] { Media._ID, Media.DISPLAY_NAME,
				Media.TITLE, Media.DURATION, Media.ARTIST, Media.DATA };
		Cursor cursor = getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, music, null, null,
				null);
		while (cursor.moveToNext()) {
			 Music temp = new Music();
			temp.setFilename(cursor.getString(1));
			temp.setTitle(cursor.getString(2));
			temp.setDuration(cursor.getInt(3));
			temp.setArtist(cursor.getString(4));
			temp.setData(cursor.getString(5));
			musiclists.add(temp);

			Map<String, Object> map = new HashMap<String, Object>();
			map.put("name", cursor.getString(1));
			map.put("artist", cursor.getString(4));
			list.add(map);
		}
		InitListView();
	}

	private void InitListView() {
		listview = (ListView) findViewById(R.id.main_list_view);
		adapter = new SimpleAdapter(this, list, R.layout.musicsshow,
				new String[] { "name", "artist" }, new int[] { R.id.name,
						R.id.artist });
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					final int current, long id) {

				new Thread() {
					public void run() {
						player.playUrl(musiclists.get(current).getData());
					};
				}.start();
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (player != null) {
			player.stop();
			player = null;
		}
	}

}
